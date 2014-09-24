/*
 * Generated by asn1c-0.9.24 (http://lionet.info/asn1c)
 * From ASN.1 module "InformationElements"
 * 	found in "../asn/InformationElements.asn"
 * 	`asn1c -fcompound-names -fnative-types`
 */

#ifndef	_UE_Positioning_ReportingQuantity_r7_H_
#define	_UE_Positioning_ReportingQuantity_r7_H_


#include <asn_application.h>

/* Including external dependencies */
#include "UE-Positioning-MethodType.h"
#include "PositioningMethod.h"
#include "UE-Positioning-Accuracy.h"
#include <BOOLEAN.h>
#include "EnvironmentCharacterisation.h"
#include <NativeEnumerated.h>
#include <BIT_STRING.h>
#include <constr_SEQUENCE.h>

#ifdef __cplusplus
extern "C" {
#endif

/* Dependencies */
typedef enum UE_Positioning_ReportingQuantity_r7__velocityRequested {
	UE_Positioning_ReportingQuantity_r7__velocityRequested_true	= 0
} e_UE_Positioning_ReportingQuantity_r7__velocityRequested;

/* UE-Positioning-ReportingQuantity-r7 */
typedef struct UE_Positioning_ReportingQuantity_r7 {
	UE_Positioning_MethodType_t	 methodType;
	PositioningMethod_t	 positioningMethod;
	UE_Positioning_Accuracy_t	*horizontalAccuracy	/* OPTIONAL */;
	UE_Positioning_Accuracy_t	*verticalAccuracy	/* OPTIONAL */;
	BOOLEAN_t	 gps_TimingOfCellWanted;
	BOOLEAN_t	 additionalAssistanceDataReq;
	EnvironmentCharacterisation_t	*environmentCharacterisation	/* OPTIONAL */;
	long	*velocityRequested	/* OPTIONAL */;
	BIT_STRING_t	*gANSSPositioningMethods	/* OPTIONAL */;
	BIT_STRING_t	*gANSSTimingOfCellWanted	/* OPTIONAL */;
	BIT_STRING_t	*gANSSCarrierPhaseMeasurementRequested	/* OPTIONAL */;
	
	/* Context for parsing across buffer boundaries */
	asn_struct_ctx_t _asn_ctx;
} UE_Positioning_ReportingQuantity_r7_t;

/* Implementation */
/* extern asn_TYPE_descriptor_t asn_DEF_velocityRequested_9;	// (Use -fall-defs-global to expose) */
extern asn_TYPE_descriptor_t asn_DEF_UE_Positioning_ReportingQuantity_r7;

#ifdef __cplusplus
}
#endif

#endif	/* _UE_Positioning_ReportingQuantity_r7_H_ */
#include <asn_internal.h>