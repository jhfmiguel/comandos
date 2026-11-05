package com.comandos.demo;
import com.fariamiguel.app.demo.AttachmentDemoPolicy;
import java.util.*;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class SecurityAttachmentDemoPolicy extends AttachmentDemoPolicy{
    @Override public Set<String> requiredProcesses(){
        var values=new LinkedHashSet<>(super.requiredProcesses());values.add("CUSTODY");return Set.copyOf(values);
    }
    @Override public boolean checksStoredBlobs(){return false;}
}