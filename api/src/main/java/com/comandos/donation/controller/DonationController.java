package com.comandos.donation.controller;
import com.comandos.donation.dto.DonationContract.*;
import com.comandos.donation.service.DonationReceiptService;
import com.comandos.donation.service.DonationService;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/erp/donations")
public class DonationController {
 private final DonationService service; private final DonationReceiptService receipts;
 public DonationController(DonationService service,DonationReceiptService receipts){this.service=service;this.receipts=receipts;}
 @GetMapping("/stock") public Page<StockOption> stock(@RequestParam long organizationId,@RequestParam(required=false) Long unitId,@RequestParam String kind,@RequestParam(defaultValue="") String search,@RequestParam(defaultValue="0") int page){return service.stock(organizationId,unitId,kind,search,page);}
 @PostMapping public DonationView finalize(@RequestBody FinalizeRequest request){return service.finalize(request);}
 @PostMapping("/receive") public DonationView receive(@RequestBody ReceiveRequest request){return receipts.receive(request);}
 @GetMapping public Page<DonationView> list(@RequestParam long organizationId,@RequestParam(required=false) Long unitId,@RequestParam(defaultValue="0") int page){return service.list(organizationId,unitId,page);}
 @GetMapping("/{id}") public DonationView get(@PathVariable long id){return service.get(id);}
 @GetMapping("/{id}/lifecycle") public DonationLifecycleView lifecycle(@PathVariable long id){return service.lifecycle(id);}
}
