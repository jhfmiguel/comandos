package com.comandos.demo;

import com.comandos.donation.model.*;
import jakarta.persistence.EntityManager;
import java.util.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name="comandos.demo.seed",havingValue="true")
@Order(1978)
public class DonationLifecycleDemoVerifier implements ApplicationRunner {
    private final EntityManager em;
    public DonationLifecycleDemoVerifier(EntityManager em){this.em=em;}

    @Override @Transactional(readOnly=true)
    public void run(ApplicationArguments args){
        var donations=em.createQuery("select d from Donation d order by d.id",Donation.class).getResultList();
        if(donations.isEmpty())throw new IllegalStateException("Donation regression requires demo donations.");
        for(var d:donations){
            require(d.term!=null&&!d.term.isBlank(),"Donation term is required.");
            require(d.termConfirmed,"Donation term must be confirmed.");
            require(d.finalizedAt!=null,"Donation finalization timestamp is required.");
            require(d.titleTransferredAt!=null,"Donation title transfer timestamp is required.");
            String direction=d.direction==null?"OUTGOING":d.direction.toUpperCase(Locale.ROOT);
            if("INCOMING".equals(direction)){
                require("RECEIVED".equals(d.eventType),"Incoming donation must be RECEIVED.");
                require("TRANSFERRED_TO_ORGANIZATION".equals(d.titleTransferState),"Incoming donation must transfer title to the organization.");
                require(d.receivedAt!=null,"Incoming donation requires receivedAt.");
                require(d.documentReference!=null&&!d.documentReference.isBlank(),"Incoming donation requires document reference.");
            }else{
                require("OUTGOING".equals(direction),"Unsupported donation direction.");
                require("REALIZED".equals(d.eventType),"Outgoing donation must be REALIZED.");
                require("TRANSFERRED_TO_DONEE".equals(d.titleTransferState),"Outgoing donation must transfer title to the donee.");
                require(d.realizedAt!=null,"Outgoing donation requires realizedAt.");
            }
            var items=em.createQuery("select i from DonationItem i where i.donation.id=:id",DonationItem.class).setParameter("id",d.id).getResultList();
            require(!items.isEmpty(),"Donation must contain items.");
            for(var i:items){
                require(i.movement!=null,"Donation item requires a stock movement.");
                require(i.previousOwnerType!=null&&i.previousOwnerName!=null,"Donation item requires previous ownership snapshot.");
                require(i.newOwnerType!=null&&i.newOwnerName!=null,"Donation item requires new ownership snapshot.");
                require(i.quantity!=null&&i.quantity.signum()>0,"Donation item quantity must be positive.");
                if("INCOMING".equals(direction)){
                    require("DONOR".equals(i.previousOwnerType)&&"ORGANIZATION".equals(i.newOwnerType),"Incoming donation ownership path is invalid.");
                    require(i.movement.quantity!=null&&i.movement.quantity.signum()>0,"Incoming donation movement must add stock.");
                    require(Set.of("DONATION_IN","DONATION").contains(i.movement.nature),"Incoming donation movement nature is invalid.");
                }else{
                    require("ORGANIZATION".equals(i.previousOwnerType)&&"DONEE".equals(i.newOwnerType),"Outgoing donation ownership path is invalid.");
                    require(Set.of("DONATION_OUT","DONATION").contains(i.movement.nature),"Outgoing donation movement nature is invalid.");
                }
            }
        }
    }
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
}
