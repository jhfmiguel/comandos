package com.weaponsregistration.lifecycle.model;
import com.weaponsregistration.core.model.CoreEntity;import jakarta.persistence.*;import java.time.LocalDateTime;
@Entity @Table(name="erp_operation_attachment",indexes=@Index(name="idx_attachment_resource_record",columnList="resource,record_id"))
public class OperationAttachment extends CoreEntity{
 @Column(nullable=false,length=100)public String resource;@Column(name="record_id",nullable=false)public Long recordId;@Column(nullable=false,length=255)public String fileName;
 @Column(nullable=false,length=255)public String contentType;@Basic(fetch=FetchType.LAZY)@Column(nullable=false,columnDefinition="bytea")public byte[] content;@Column(length=1000)public String description;
 @Column(nullable=false)public LocalDateTime uploadedAt;@Column public Long uploadedById;@Column public String uploadedByLogin;
}