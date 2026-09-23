package com.comandos.lifecycle.model;
import com.comandos.core.model.CoreEntity;import jakarta.persistence.*;import java.time.LocalDateTime;
@Entity @Table(name="erp_operation_attachment",indexes=@Index(name="idx_attachment_resource_record",columnList="resource_name,record_id"))
public class OperationAttachment extends CoreEntity{
 @Column(name="resource_name",nullable=false,length=100)public String resource;@Column(name="record_id",nullable=false)public Long recordId;@Column(nullable=false,length=255)public String fileName;
 @Column(nullable=false,length=255)public String contentType;@Lob@Basic(fetch=FetchType.LAZY)@Column(nullable=false)public byte[] content;@Column(length=1000)public String description;
 @Column(nullable=false)public LocalDateTime uploadedAt;@Column public Long uploadedById;@Column public String uploadedByLogin;
}