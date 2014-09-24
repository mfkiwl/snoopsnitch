/*
 * Generated by asn1c-0.9.24 (http://lionet.info/asn1c)
 * From ASN.1 module "InformationElements"
 * 	found in "../asn/InformationElements.asn"
 * 	`asn1c -fcompound-names -fnative-types`
 */

#include "RSR-VCC-Info.h"

static int
memb_nonce_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
			asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	const BIT_STRING_t *st = (const BIT_STRING_t *)sptr;
	size_t size;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
	
	if(st->size > 0) {
		/* Size in bits */
		size = 8 * st->size - (st->bits_unused & 0x07);
	} else {
		size = 0;
	}
	
	if((size == 128)) {
		/* Constraint check succeeded */
		return 0;
	} else {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: constraint failed (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
}

static int
memb_ims_Information_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
			asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	const OCTET_STRING_t *st = (const OCTET_STRING_t *)sptr;
	size_t size;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
	
	size = st->size;
	
	if((size >= 1 && size <= 32)) {
		/* Constraint check succeeded */
		return 0;
	} else {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: constraint failed (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
}

static asn_per_constraints_t asn_PER_memb_nonce_constr_2 = {
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 },
	{ APC_CONSTRAINED,	 0,  0,  128,  128 }	/* (SIZE(128..128)) */,
	0, 0	/* No PER value map */
};
static asn_per_constraints_t asn_PER_memb_ims_Information_constr_3 = {
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 },
	{ APC_CONSTRAINED,	 5,  5,  1,  32 }	/* (SIZE(1..32)) */,
	0, 0	/* No PER value map */
};
static asn_TYPE_member_t asn_MBR_RSR_VCC_Info_1[] = {
	{ ATF_POINTER, 1, offsetof(struct RSR_VCC_Info, nonce),
		(ASN_TAG_CLASS_CONTEXT | (0 << 2)),
		-1,	/* IMPLICIT tag at current level */
		&asn_DEF_BIT_STRING,
		memb_nonce_constraint_1,
		&asn_PER_memb_nonce_constr_2,
		0,
		"nonce"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct RSR_VCC_Info, ims_Information),
		(ASN_TAG_CLASS_CONTEXT | (1 << 2)),
		-1,	/* IMPLICIT tag at current level */
		&asn_DEF_OCTET_STRING,
		memb_ims_Information_constraint_1,
		&asn_PER_memb_ims_Information_constr_3,
		0,
		"ims-Information"
		},
};
static int asn_MAP_RSR_VCC_Info_oms_1[] = { 0 };
static ber_tlv_tag_t asn_DEF_RSR_VCC_Info_tags_1[] = {
	(ASN_TAG_CLASS_UNIVERSAL | (16 << 2))
};
static asn_TYPE_tag2member_t asn_MAP_RSR_VCC_Info_tag2el_1[] = {
    { (ASN_TAG_CLASS_CONTEXT | (0 << 2)), 0, 0, 0 }, /* nonce at 2130 */
    { (ASN_TAG_CLASS_CONTEXT | (1 << 2)), 1, 0, 0 } /* ims-Information at 2131 */
};
static asn_SEQUENCE_specifics_t asn_SPC_RSR_VCC_Info_specs_1 = {
	sizeof(struct RSR_VCC_Info),
	offsetof(struct RSR_VCC_Info, _asn_ctx),
	asn_MAP_RSR_VCC_Info_tag2el_1,
	2,	/* Count of tags in the map */
	asn_MAP_RSR_VCC_Info_oms_1,	/* Optional members */
	1, 0,	/* Root/Additions */
	-1,	/* Start extensions */
	-1	/* Stop extensions */
};
asn_TYPE_descriptor_t asn_DEF_RSR_VCC_Info = {
	"RSR-VCC-Info",
	"RSR-VCC-Info",
	SEQUENCE_free,
	SEQUENCE_print,
	SEQUENCE_constraint,
	SEQUENCE_decode_ber,
	SEQUENCE_encode_der,
	SEQUENCE_decode_xer,
	SEQUENCE_encode_xer,
	SEQUENCE_decode_uper,
	SEQUENCE_encode_uper,
	0,	/* Use generic outmost tag fetcher */
	asn_DEF_RSR_VCC_Info_tags_1,
	sizeof(asn_DEF_RSR_VCC_Info_tags_1)
		/sizeof(asn_DEF_RSR_VCC_Info_tags_1[0]), /* 1 */
	asn_DEF_RSR_VCC_Info_tags_1,	/* Same as above */
	sizeof(asn_DEF_RSR_VCC_Info_tags_1)
		/sizeof(asn_DEF_RSR_VCC_Info_tags_1[0]), /* 1 */
	0,	/* No PER visible constraints */
	asn_MBR_RSR_VCC_Info_1,
	2,	/* Elements count */
	&asn_SPC_RSR_VCC_Info_specs_1	/* Additional specs */
};
