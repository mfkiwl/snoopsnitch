/*
 * Generated by asn1c-0.9.24 (http://lionet.info/asn1c)
 * From ASN.1 module "InformationElements"
 * 	found in "../asn/InformationElements.asn"
 * 	`asn1c -fcompound-names -fnative-types`
 */

#ifndef	_UE_RadioAccessCapabBandFDDList3_H_
#define	_UE_RadioAccessCapabBandFDDList3_H_


#include <asn_application.h>

/* Including external dependencies */
#include <asn_SEQUENCE_OF.h>
#include <constr_SEQUENCE_OF.h>

#ifdef __cplusplus
extern "C" {
#endif

/* Forward declarations */
struct UE_RadioAccessCapabBandFDD3;

/* UE-RadioAccessCapabBandFDDList3 */
typedef struct UE_RadioAccessCapabBandFDDList3 {
	A_SEQUENCE_OF(struct UE_RadioAccessCapabBandFDD3) list;
	
	/* Context for parsing across buffer boundaries */
	asn_struct_ctx_t _asn_ctx;
} UE_RadioAccessCapabBandFDDList3_t;

/* Implementation */
extern asn_TYPE_descriptor_t asn_DEF_UE_RadioAccessCapabBandFDDList3;

#ifdef __cplusplus
}
#endif

/* Referred external types */
#include "UE-RadioAccessCapabBandFDD3.h"

#endif	/* _UE_RadioAccessCapabBandFDDList3_H_ */
#include <asn_internal.h>