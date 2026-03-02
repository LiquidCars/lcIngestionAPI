package net.liquidcars.ingestion.domain.model.security;

import java.util.List;

public enum AccessRoleEnum {
    LCAdmin(null),
    LCSupport(List.of(LCAdmin)),
    TPAdmin(List.of(LCAdmin)),
    TPCarSeller(null),
    TPSupport(List.of(TPAdmin)),
    LCCommercial(List.of(LCAdmin)),
    LCBusiness(List.of(LCSupport, LCCommercial)),
    TPCommercial(List.of(TPAdmin)),
    TPBusiness(List.of(TPCommercial, TPSupport)),
    LCReadOnly(List.of(LCBusiness)),
    TPServiceProvider(null),
    TPFintech(List.of(TPServiceProvider)),
    TPCarGuarantee(List.of(TPServiceProvider)),
    TPConsumerFinance(List.of(TPServiceProvider)),
    TPOwnershipTransfer(List.of(TPServiceProvider)),
    TPCarShipping(List.of(TPServiceProvider)),
    TPInsurance(List.of(TPServiceProvider)),
    TPReservationSystem(List.of(TPServiceProvider)),
    TPOnlineChannelProduct(List.of(TPServiceProvider)),
    TPReadOnly(List.of(TPBusiness)),
    AuthenticatedUser(List.of(TPReadOnly, LCReadOnly)),
    M2M(null),
    M2MSystemPartner(List.of(M2M)),
    M2MB2BOnlineChannel(List.of(M2M)),
    M2MCarSeller(List.of(M2M)),
    M2MServiceProvider(List.of(M2M)),
    M2MLiquidCarsModule(List.of(M2M)),
    M2MB2COnlineChannel(List.of(M2M));


    public final static String LCAdmin_role = "LCAdmin";
    public final static String LCSupport_role = "LCSupport";
    public final static String LCCommercial_role = "LCCommercial";
    public final static String LCBusiness_role = "LCBusiness";
    public final static String TPCarSeller_role = "TPCarSeller";
    public final static String TPAdmin_role = "TPAdmin";
    public final static String TPSupport_role = "TPSupport";
    public final static String TPCommercial_role = "TPCommercial";
    public final static String TPBusiness_role = "TPBusiness";
    public final static String TPReadOnly_role = "TPReadOnly";
    public final static String AuthenticatedUser_role = "AuthenticatedUser";
    public final static String M2M_role = "M2M";
    public final static String M2MB2BOnlineChannel_role = "M2MB2BOnlineChannel";
    public final static String M2MCarSeller_role = "M2MCarSeller";
    public final static String M2MServiceProvider_role = "M2MServiceProvider";
    public final static String M2MLiquidCarsModule_role = "M2MLiquidCarsModule";
    public final static String M2MB2COnlineChannel_role = "M2MB2COnlineChannel";



    private final List<AccessRoleEnum> parents;
    AccessRoleEnum(List<AccessRoleEnum> parent){
        this.parents = parent;
    }
    public boolean allows(AccessRoleEnum checkedRole){
        if (checkedRole==null) return false;
        if(this.equals(checkedRole)) return true;
        if (parents!=null && !parents.isEmpty()) {
            for (AccessRoleEnum role : parents) {
                if (role.equals(checkedRole)) return true;
                if (role.allows(checkedRole)) return true;
            }
        }
        return false;
    }
    public List<AccessRoleEnum> getParents(){
        return parents;
    }
}