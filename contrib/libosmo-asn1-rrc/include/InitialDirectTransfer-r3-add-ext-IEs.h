/*
 * Generated by asn1c-0.9.24 (http://lionet.info/asn1c)
 * From ASN.1 module "PDU-definitions"
 * 	found in "../asn/PDU-definitions.asn"
 * 	`asn1c -fcompound-names -fnative-types`
 */

#ifndef	_InitialDirectTransfer_r3_add_ext_IEs_H_
#define	_InitialDirectTransfer_r3_add_ext_IEs_H_


#include <asn_application.h>

/* Including external dependencies */
#include "InitialDirectTransfer-v7g0ext-IEs.h"
#include <constr_SEQUENCE.h>

#ifdef __cplusplus
extern "C" {
#endif

/* InitialDirectTransfer-r3-add-ext-IEs */
typedef struct InitialDirectTransfer_r3_add_ext_IEs {
	InitialDirectTransfer_v7g0ext_IEs_t	 initialDirectTransfer_v7g0ext;
	struct InitialDirectTransfer_r3_add_ext_IEs__nonCriticalExtensions {
		
		/* Context for parsing across buffer boundaries */
		asn_struct_ctx_t _asn_ctx;
	} *nonCriticalExtensions;
	
	/* Context for parsing across buffer boundaries */
	asn_struct_ctx_t _asn_ctx;
} InitialDirectTransfer_r3_add_ext_IEs_t;

/* Implementation */
extern asn_TYPE_descriptor_t asn_DEF_InitialDirectTransfer_r3_add_ext_IEs;

#ifdef __cplusplus
}
#endif

#endif	/* _InitialDirectTransfer_r3_add_ext_IEs_H_ */
#include <asn_internal.h>