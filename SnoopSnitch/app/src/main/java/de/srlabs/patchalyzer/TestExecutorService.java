package de.srlabs.patchalyzer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

public class TestExecutorService extends Service {
    private JSONObject deviceInfoJson = null;
    private TestSuite testSuite = null;
    private DeviceInfoThread deviceInfoThread = null;
    Set<Thread> subThreads = null;
    private BasicTestCache basicTestCache = null;
    private ITestExecutorCallbacks callback = null;
    private boolean apiRunning = false;
    private boolean basicTestsRunning = false;
    private boolean deviceInfoRunning = false;
    private boolean downloadingTestSuite = false;
    private Handler handler = null;
    private ServerApi api = null;
    private SharedPreferences sharedPrefs;
    private Vector<ProgressItem> progressItems;
    public static final String NO_INTERNET_CONNECTION_ERROR = "no_uplink";

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(Constants.LOG_TAG,"onCreate() of TestExecutorService called...");

        handler = new Handler();
        api = new ServerApi();
        subThreads = new HashSet<Thread>();

        try {
            this.sharedPrefs = getSharedPreferences("TestSuite", Context.MODE_PRIVATE);
        } catch (Exception e) {
            Log.e(Constants.LOG_TAG, "Exception in TestExecutorService()",e);
        }
    }

    private void downloadTestSuite(final ProgressItem downloadTestSuiteProgress, final ProgressItem parseTestSuiteProgress){
        downloadingTestSuite = true;
        Thread thread = new Thread(){
            @Override
            public void run(){
                ServerApi api = new ServerApi();
                try{
                    //FIXME add current testVersion here ->
                    if(!isConnectedToInternet()){
                        return;
                    }
                    Log.d(Constants.LOG_TAG,"Downloading testsuite from server...");
                    File f = api.downloadTestSuite("newtestsuite",TestExecutorService.this,TestUtils.getAppId(TestExecutorService.this), Build.VERSION.SDK_INT,"0", Constants.APP_VERSION);
                    downloadTestSuiteProgress.update(1.0);
                    Log.d(Constants.LOG_TAG,"Finished downloading testsuite JSON to file:"+f.getAbsolutePath());
                    downloadingTestSuite = false;
                    parseTestSuiteFile(f,parseTestSuiteProgress);
                    checkIfCVETestsAvailable(testSuite);

                }catch(Exception e){
                    Log.e(Constants.LOG_TAG,"Exception while downloading teststuite to file!"+e.getMessage());
                    try {
                        callback.showErrorMessage("Exception while downloading testsuite: " + e.getMessage());
                    }catch(RemoteException ex){
                        Log.e(Constants.LOG_TAG,"RemoteException when trying to show error message: "+ex.getMessage());
                    }
                }
            }
        };
        thread.start();
    }

    private void parseTestSuiteFile(File testSuiteFile,final ProgressItem parseTestSuiteProgress){
        Log.d(Constants.LOG_TAG,"TestExecutorService: Parsing testsuite...");
        Log.d(Constants.LOG_TAG, "TestExecutorService: testSuiteFile:" + testSuiteFile.getAbsolutePath());
        testSuite = new TestSuite(this, testSuiteFile);
        parseTestSuiteProgress.update(0.3);
        testSuite.addBasicTestsToDB();
        parseTestSuiteProgress.update(0.6);
        testSuite.parseVulnerabilites();
        parseTestSuiteProgress.update(1.0);
        showStatus("Parsing testsuite finished...");
        basicTestCache = new BasicTestCache(this, testSuite.getVersion(), Build.VERSION.SDK_INT);
        Log.d(Constants.LOG_TAG,"TestExecutorService: Finished parsing testsuite!");

    }

    private boolean isAppOutdated(boolean notify){
        if(testSuite != null && testSuite.getMinAppVersion() != -1){
            int minAppVersion = testSuite.getMinAppVersion();
            Log.i(Constants.LOG_TAG, "isAppOutdated(): Found minAppVersion=" + minAppVersion);

            boolean outdated = minAppVersion > Constants.APP_VERSION;
            //outdated = true;
            if( notify && outdated ){
                String upgradeUrlTmp = null;
                if(testSuite.getUpdradeUrl() != null){

                        upgradeUrlTmp = testSuite.getUpdradeUrl();
                }
                final String upgradeUrl = upgradeUrlTmp;
                handler.post(new Runnable(){
                    @Override
                    public void run() {
                        try {
                            callback.showOutdatedError(upgradeUrl);
                        } catch (RemoteException e) {
                            Log.e(Constants.LOG_TAG, "isAppOutdated RemoteException", e);
                        }
                    }
                });
            }
            return outdated;
        } else{
            return false;
        }
    }

    private String getCurrentTestVersion() throws JSONException{
        return testSuite.getVersion();
    }

    private final ITestExecutorServiceInterface.Stub mBinder = new ITestExecutorServiceInterface.Stub() {
        @Override
        public void startMakingDeviceInfo() throws RemoteException {
            if(deviceInfoThread != null && deviceInfoThread.isAlive()){
                return;
            }
            if(deviceInfoJson != null)
                deviceInfoJson = null;
            deviceInfoThread = new DeviceInfoThread(new ProgressItem(null, "DUMMY", 1.0), null);
            deviceInfoThread.start();
        }

        @Override
        public boolean isDeviceInfoFinished() throws RemoteException {
            if(deviceInfoThread != null && deviceInfoThread.isAlive()){
                return false;
            }
            return deviceInfoJson != null;
        }

        @Override
        public String getDeviceInfoJson() throws RemoteException {
            try {
                return deviceInfoJson.toString(4);
            } catch (JSONException e) {
                Log.e(Constants.LOG_TAG, "JSONException in getDeviceInfoJson()", e);
                return e.toString();
            }
        }

        @Override
        public void startBasicTests() throws RemoteException {
            basicTestCache.startWorking();
        }

        @Override
        public int getBasicTestsQueueSize() throws RemoteException {
            return basicTestCache.getQueueSize();
        }
        public String evaluateVulnerabilitiesTests() throws RemoteException{
            try {
                Log.d(Constants.LOG_TAG,"Starting to create result JSON...");
                boolean is64BitSystem = TestUtils.is64BitSystem();
                JSONObject result = new JSONObject();
                JSONObject vulnerabilities = testSuite.getVulnerabilities();
                Iterator<String> identifierIterator = vulnerabilities.keys();
                Vector<String> identifiers = new Vector<String>();
                while(identifierIterator.hasNext())
                    identifiers.add(identifierIterator.next());
                Collections.sort(identifiers);
                Log.d(Constants.LOG_TAG,"number of vulnerabilities to test: "+identifiers.size());
                for(String identifier:identifiers){
                    JSONObject vulnerability = vulnerabilities.getJSONObject(identifier);

                    try {
                        Boolean testRequires64Bit = vulnerability.getBoolean("testRequires64bit");
                        if (!is64BitSystem && testRequires64Bit) {
                            //test will not be possible; skip test
                            //FIXME how to display test
                            continue;
                        }
                    }catch(JSONException e){
                        //ignoring exception here, as the old test might not come with this info
                    }

                    String category = vulnerability.getString("category");
                    JSONObject test_not_affected = vulnerability.getJSONObject("testNotAffected");
                    if(test_not_affected == null)
                        Log.d(Constants.LOG_TAG,"test_not_affected == null");
                    Boolean notAffected = TestEngine.runTest(basicTestCache, test_not_affected);
                    if(notAffected == null)
                        Log.d(Constants.LOG_TAG,"notAffected == null");
                    JSONObject vulnerabilityResult = new JSONObject();
                    vulnerabilityResult.put("identifier", identifier);
                    vulnerabilityResult.put("title", vulnerability.getString("title"));
                    vulnerabilityResult.put("notAffected", notAffected);
                    if(!notAffected){
                        JSONObject testVulnerable = vulnerability.getJSONObject("testVulnerable");
                        Boolean vulnerable = TestEngine.runTest(basicTestCache, testVulnerable);
                        vulnerabilityResult.put("vulnerable", vulnerable);
                        JSONObject testFixed = vulnerability.getJSONObject("testFixed");
                        Boolean fixed = TestEngine.runTest(basicTestCache, testFixed);
                        vulnerabilityResult.put("fixed", fixed);
                    }
                    if(!result.has(category)){
                        result.put(category, new JSONArray());
                    }
                    result.getJSONArray(category).put(vulnerabilityResult);
                }

                return result.toString(4);
            } catch(Exception e){
                Log.e(Constants.LOG_TAG, "Exception in evaluateVulnerabilitiesTests",e);
                return e.toString();
            }
        }
        @Override
        public void clearCache(){
            basicTestCache.clearCache();
        }

        @Override
        public boolean updateTestsNeeded() throws RemoteException {
            return true;
        }

        @Override
        public void startWork(boolean updateTests, boolean generateDeviceInfo, final boolean evaluateTests, final boolean uploadTestResults, final boolean uploadDeviceInfo, final ITestExecutorCallbacks callback){

            TestExecutorService.this.callback = callback;
            if(downloadingTestSuite){
                showStatus("Still downloading test suite...please be patient!");
                return;
            }
            if(isAppOutdated(true)){
                return;
            }
            if(apiRunning){
                showStatus("Already work in progress, not starting: apiRunning");
                return;
            }
            if(basicTestsRunning) {
                showStatus("Already work in progress, not starting: basicTestsRunning");
                return;
            }
            if(deviceInfoRunning){
                showStatus("Already work in progress, not starting: deviceInfoRunning");
                return;
            }

            clearProgress();
            updateProgress();

            final ProgressItem uploadDeviceInfoProgress;
            if(uploadDeviceInfo) {
                uploadDeviceInfoProgress = addProgressItem("uploadDeviceInfo", 2.0);
            } else{
                uploadDeviceInfoProgress = null;
            }
            final ProgressItem uploadTestResultsProgress;
            if(uploadTestResults) {
                uploadTestResultsProgress = addProgressItem("uploadTestResults", 1.0);
            } else{
                uploadTestResultsProgress = null;
            }
            final ProgressItem basicTestsProgress;
            if(evaluateTests) {
                basicTestsProgress = addProgressItem("basicTests", 1);
            } else{
                basicTestsProgress = null;
            }
            final ProgressItem downloadTestSuiteProgress;
            final ProgressItem parseTestSuiteProgress;
            if(updateTests){
                downloadTestSuiteProgress = addProgressItem("downloadTestSuite", 1);
                parseTestSuiteProgress = addProgressItem("parseTestSuite",1.5);
            }
            else{
                downloadTestSuiteProgress = null;
                parseTestSuiteProgress = null;
            }
            updateProgress();
            if(true){
                ProgressItem downloadRequestsProgress = addProgressItem("downloadRequests", 0.5);
                Thread requestsThread = new RequestsThread(downloadRequestsProgress);
                subThreads.add(requestsThread);
                requestsThread.start();
            }
            final Runnable pendingTestResultsUploadRunnable = new Runnable(){
                @Override
                public void run() {
                    basicTestsRunning = false;
                    try{
                        if(uploadTestResults){
                            apiRunning = true;
                            if(!isConnectedToInternet()){
                                return;
                            }
                            api.reportTest(basicTestCache.toJson(), TestUtils.getAppId(TestExecutorService.this), TestUtils.getDeviceModel(), TestUtils.getBuildFingerprint(), TestUtils.getBuildDisplayName(), TestUtils.getBuildDateUtc(), Constants.APP_VERSION);
                            showStatus("Uploading test results finished...");
                            uploadTestResultsProgress.update(1.0);
                            apiRunning = false;
                        }
                    } catch(IOException e){
                        reportError(NO_INTERNET_CONNECTION_ERROR);
                        stopSubThreads();
                        Log.e(Constants.LOG_TAG, "Exception in pendingTestResultsUploadRunnable", e);
                        apiRunning = false;
                    } catch( JSONException e){
                        Log.e(Constants.LOG_TAG,"JSONException in pendingTestResultsUploadRunnable: "+e.getMessage());
                        apiRunning = false;
                    }
                }
            };
            final Runnable pendingDeviceInfoUploadRunnable = new Runnable(){
                @Override
                public void run() {
                    basicTestsRunning = false;
                    try{
                        if(uploadDeviceInfo){
                            apiRunning = true;
                            if (deviceInfoJson != null) {
                                if(!isConnectedToInternet()){
                                    return;
                                }
                                api.reportSys(deviceInfoJson, TestUtils.getAppId(TestExecutorService.this), TestUtils.getDeviceModel(), TestUtils.getBuildFingerprint(), TestUtils.getBuildDisplayName(), TestUtils.getBuildDateUtc(), Constants.APP_VERSION);
                            }
                            showStatus("Uploading device info finished...");
                            uploadDeviceInfoProgress.update(1.0);
                            apiRunning = false;
                        }
                    } catch(IOException e){
                        reportError(NO_INTERNET_CONNECTION_ERROR);
                        stopSubThreads();
                        Log.e(Constants.LOG_TAG, "Exception in pendingDeviceInfoUploadRunnable()", e);
                        apiRunning = false;
                    } catch(JSONException e){
                        Log.e(Constants.LOG_TAG,"JSONException in pendingDeviceInfoUploadRunnable: "+e.getMessage());
                        apiRunning = false;
                    }
                }
            };
            if(generateDeviceInfo) {
                ProgressItem deviceInfoProgress = addProgressItem("deviceInfo", 2);
                // Run sysinfo in background
                if (deviceInfoThread != null && deviceInfoThread.isAlive()) {
                    return;
                }
                if (deviceInfoJson != null)
                    deviceInfoJson = null;
                deviceInfoThread = new DeviceInfoThread(deviceInfoProgress, pendingDeviceInfoUploadRunnable);
                subThreads.add(deviceInfoThread);
                Log.i(Constants.LOG_TAG, "Starting DeviceInfoThread");
                deviceInfoRunning = true;
                deviceInfoThread.start();
            }
            if(updateTests){
                Thread downloadAndParseTestSuiteThread = new DownloadThread(downloadTestSuiteProgress, parseTestSuiteProgress,evaluateTests, basicTestsProgress, pendingTestResultsUploadRunnable);
                downloadAndParseTestSuiteThread.start();
                subThreads.add(downloadAndParseTestSuiteThread);
            } else{
                if(evaluateTests){
                    Log.i(Constants.LOG_TAG, "Calling basicTestCache.startTesting()");
                    basicTestCache.startTesting(basicTestsProgress, pendingTestResultsUploadRunnable);
                }
            }
        }

        @Override
        public void upload(final boolean uploadTestResults, final boolean uploadDeviceInfo, ITestExecutorCallbacks callback) throws RemoteException {
            if(apiRunning){
                try {
                    callback.showErrorMessage("Already work in progress, not starting: apiRunning");
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                return;
            }
            TestExecutorService.this.callback = callback;
            apiRunning = true;
            Thread t = new Thread(){
                @Override
                public void run() {
                    try {
                        sendProgressToCallback(0.0);
                        if (uploadTestResults) {
                            api.reportTest(basicTestCache.toJson(), TestUtils.getAppId(TestExecutorService.this), TestUtils.getDeviceModel(), TestUtils.getBuildFingerprint(), TestUtils.getBuildDisplayName(), TestUtils.getBuildDateUtc(), Constants.APP_VERSION);
                        }
                        sendProgressToCallback(0.5);
                        if (uploadDeviceInfo && deviceInfoJson != null) {
                            api.reportSys(deviceInfoJson, TestUtils.getAppId(TestExecutorService.this), TestUtils.getDeviceModel(), TestUtils.getBuildFingerprint(), TestUtils.getBuildDisplayName(), TestUtils.getBuildDateUtc(), Constants.APP_VERSION);
                        }
                        sendProgressToCallback(1.0);
                    } catch(Exception e){
                        reportError("Exception in upload(): " + e);
                        Log.e(Constants.LOG_TAG, "Exception in upload()", e);
                    }
                    apiRunning = false;
                    sendFinishedToCallback();
                }
            };
            t.start();
        }
    };
    private void checkIfCVETestsAvailable(TestSuite testSuite) {
        if(testSuite != null){
            String noCVETestsMessage = testSuite.getNoCVETestMessage();
            if(noCVETestsMessage != null){
                //show dialog in UI after testing
                showNoCVETestsForApiLevel(noCVETestsMessage);
            }
        }
        else{
            Log.e(Constants.LOG_TAG,"checkIfCVETestsAvailable: testSuite is null");
        }
    }
    private void clearProgress(){
        this.progressItems = new Vector<ProgressItem>();
    }
    private ProgressItem addProgressItem(String name, double weight){
        Log.d(Constants.LOG_TAG,"Adding progressItem: "+name+" weight:"+weight);
        ProgressItem item = new ProgressItem(this, name, weight);
        progressItems.add(item);
        return item;
    }
    private double getTotalProgress(){
        //Log.i(Constants.LOG_TAG, "getTotalProgres()");
        double weightSum = 0;
        double progressSum = 0;
        for(ProgressItem progressItem:progressItems){
            Log.i(Constants.LOG_TAG, "getTotalProgres(): name=" + progressItem.getName() + "  weight=" + progressItem.getWeight() + "  progress=" + progressItem.getProgress());
            weightSum += progressItem.getWeight();
            progressSum += progressItem.getProgress() * progressItem.getWeight();
        }
        double totalProgress;
        if(weightSum == 0){
            totalProgress = 0;
        } else {
            totalProgress = progressSum / weightSum;
        }
        //Log.i(Constants.LOG_TAG, "getTotalProgres() returns "+ totalProgress);
        return totalProgress;
    }
    public void updateProgress() {
        double totalProgress = getTotalProgress();
        sendProgressToCallback(totalProgress);
        if(totalProgress == 1.0) {
            sendFinishedToCallback();
        }
    }
    private void sendFinishedToCallback(){
        handler.post(new Runnable(){
            @Override
            public void run() {
                try {
                    callback.finished();
                } catch (RemoteException e) {
                    Log.e(Constants.LOG_TAG, "TestExecutorService.updateProgress() => callback.finished() RemoteException", e);
                }
            }
        });
    }
    private void sendProgressToCallback(final double totalProgress){
        handler.post(new Runnable(){
            @Override
            public void run() {
                try {
                    callback.updateProgress(totalProgress);
                } catch (RemoteException e) {
                    Log.e(Constants.LOG_TAG, "TestExecutorService.updateProgress() RemoteException", e);
                }
            }
        });
    }
    private void reportError(final String error){
        handler.post(new Runnable(){
            @Override
            public void run() {
                try {
                    callback.showErrorMessage(error);
                } catch (RemoteException e) {
                    Log.e(Constants.LOG_TAG, "TestExecutorService.reportError() RemoteException", e);
                }
            }
        });
    }
    private void showStatus(final String status){
        handler.post(new Runnable(){
            @Override
            public void run() {
                try {
                    callback.showStatusMessage(status);
                } catch (RemoteException e) {
                    Log.e(Constants.LOG_TAG, "TestExecutorService.showStatus() RemoteException", e);
                }
            }
        });
    }
    private void showNoCVETestsForApiLevel(final String message){
        handler.post(new Runnable(){
            @Override
            public void run() {
                try {
                    callback.showNoCVETestsForApiLevel(message);
                } catch (RemoteException e) {
                    Log.e(Constants.LOG_TAG, "TestExecutorService.showNoCVETestsForApiLevel() RemoteException", e);
                }
            }
        });
    }

    private class DownloadThread extends Thread{
        private ProgressItem downloadProgress;
        private ProgressItem parsingProgress;
        private boolean evaluateTests = false;
        private ProgressItem basicTestsProgress = null;
        private Runnable pendingTestResultsUploadRunnable = null;

        public DownloadThread(ProgressItem downloadProgress, ProgressItem parsingProgress, boolean evaluateTests, ProgressItem basicTestsProgress, Runnable pendingTestResultsUploadRunnable){
            this.evaluateTests = evaluateTests;
            this.downloadProgress = downloadProgress;
            this.parsingProgress = parsingProgress;
            this.basicTestsProgress = basicTestsProgress;
            this.pendingTestResultsUploadRunnable = pendingTestResultsUploadRunnable;
        }

        @Override
        public void run(){
            try {
                Log.i(Constants.LOG_TAG, "Starting download test suite thread...");
                apiRunning = true;
                //FIXME add current testVersion here ->
                if(!isConnectedToInternet()){
                    return;
                }
                downloadingTestSuite = true;
                Log.d(Constants.LOG_TAG,"Downloading testsuite from server...");
                File f = api.downloadTestSuite("newtestsuite",TestExecutorService.this,TestUtils.getAppId(TestExecutorService.this), Build.VERSION.SDK_INT,"0", Constants.APP_VERSION);
                showStatus("Downloading testsuite finished...");
                downloadProgress.update(1.0);
                Log.d(Constants.LOG_TAG,"Finished downloading testsuite JSON to file:"+f.getAbsolutePath());
                downloadingTestSuite = false;
                parseTestSuiteFile(f,parsingProgress);
                if(isAppOutdated(true)) {
                    return;
                }
                checkIfCVETestsAvailable(testSuite);

            } catch (JSONException e) {
                Log.e(Constants.LOG_TAG, "JSONException in DownloadThread", e);
                reportError("JSONException in DownloadThread" + e);
                return;
            } catch (IOException e) {
                Log.e(Constants.LOG_TAG, "IOException in DownloadThread", e);
                reportError(NO_INTERNET_CONNECTION_ERROR);
                stopSubThreads();
                return;
            } finally{
                apiRunning = false;
            }
            Log.i(Constants.LOG_TAG, "Calling basicTestCache.startTesting()");
            if(evaluateTests) {
                basicTestsRunning = true;
                basicTestCache.startTesting(basicTestsProgress, pendingTestResultsUploadRunnable);
            }
        }

    }

    private class DeviceInfoThread extends Thread{
        private ProgressItem progress;
        private Runnable onFinishedRunnable;

        public DeviceInfoThread(ProgressItem progress, Runnable onFinishedRunnable){
            this.progress = progress;
            this.onFinishedRunnable = onFinishedRunnable;
        }

        public void doNotRunFinishedRunnable(){
            this.onFinishedRunnable = null;
        }

        @Override
        public void run() {
            if(deviceInfoJson == null)
                deviceInfoJson = TestUtils.makeDeviceinfoJson(TestExecutorService.this, progress);
            deviceInfoThread = null;
            deviceInfoRunning = false;
            if(onFinishedRunnable != null) {
                onFinishedRunnable.run();
            }
        }
    }
    private class RequestsThread extends Thread{
        private ProgressItem downloadRequestsProgress;
        public RequestsThread(ProgressItem downloadRequestsProgress){
            this.downloadRequestsProgress = downloadRequestsProgress;
        }
        @Override
        public void run(){
            Vector<ProgressItem> requestProgress = new Vector<>();
            try {
                if(!isConnectedToInternet()){
                    return;
                }
                JSONArray requestsJson = api.getRequests(TestUtils.getAppId(TestExecutorService.this), Build.VERSION.SDK_INT, TestUtils.getDeviceModel(), TestUtils.getBuildFingerprint(), TestUtils.getBuildDisplayName(), TestUtils.getBuildDateUtc(), Constants.APP_VERSION);
                downloadRequestsProgress.update(1.0);
                showStatus("Downloading requests finished...");
                for(int i=0;i<requestsJson.length();i++){
                    Log.i(Constants.LOG_TAG, "Adding progress item for request " + i);
                    requestProgress.add(addProgressItem("Request_" + i, 1.0));
                }
                updateProgress();
                for(int i=0;i<requestsJson.length();i++){
                    JSONObject request = requestsJson.getJSONObject(i);
                    String requestType = request.getString("requestType");
                    if(requestType.equals("UPLOAD_FILE")){
                        String filename = request.getString("filename");
                        TestUtils.validateFilename(filename);
                        Log.d(Constants.LOG_TAG,"Uploading file: "+filename);
                        if(!isConnectedToInternet()){
                            return;
                        }
                        api.reportFile(filename, TestUtils.getAppId(TestExecutorService.this), TestUtils.getDeviceModel(), TestUtils.getBuildFingerprint(), TestUtils.getBuildDisplayName(), TestUtils.getBuildDateUtc(), Constants.APP_VERSION);
                        requestProgress.get(i).update(1.0);
                        updateProgress();
                    } else{
                        Log.e(Constants.LOG_TAG, "Received invalid request type " + requestType);
                        requestProgress.get(i).update(1.0);
                        updateProgress();
                    }
                }
                showStatus("Reporting files to server finished...");
            } catch(Exception e){
                Log.e(Constants.LOG_TAG, "RequestsThread.run() exception", e);
                if(e instanceof IOException) {
                    stopSubThreads();
                    reportError(NO_INTERNET_CONNECTION_ERROR);
                }
                downloadRequestsProgress.update(1.0);
                for(ProgressItem x:requestProgress){
                    x.update(1.0);
                }
                updateProgress();
            }

        }
    }

    private boolean isConnectedToInternet(){
        if(!TestUtils.isConnectedToInternet(TestExecutorService.this)){
            reportError(NO_INTERNET_CONNECTION_ERROR);
            stopSubThreads();
            return false;
        }
        else{
            return true;
        }
    }

    private void stopSubThreads(){
        //reset test status
        Log.d(Constants.LOG_TAG,"Resetting state and stopping TestExecutorService subthreads...");
        apiRunning = false;
        basicTestsRunning = false;
        deviceInfoRunning = false;
        downloadingTestSuite = false;

        for(Thread thread : subThreads){
            while(true) {
                try {
                    thread.join();
                    Log.d(Constants.LOG_TAG,"Stopped TestExecutorService subthread.");
                    break;
                } catch (InterruptedException e) {
                    Log.d(Constants.LOG_TAG, "InterruptedException while stopping running subThread: " + e.getMessage());
                }
            }
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

}
