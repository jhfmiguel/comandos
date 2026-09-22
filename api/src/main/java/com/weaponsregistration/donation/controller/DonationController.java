package com.weaponsregistration.donation.controller;
import com.weaponsregistration.donation.dto.DonationContract.*;
import com.weaponsregistration.donation.service.DonationService;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/erp/donations")
public class DonationController {
 private final DonationService service; public DonationController(DonationService service){this.service=service;}
 @GetMapping("/stock") public Page<StockOption> stock(@RequestParam long organizationId,@RequestParam(required=false) Long unitId,@RequestParam String kind,@RequestParam(defaultValue="") String search,@RequestParam(defaultValue="0") int page){return service.stock(organizationId,unitId,kind,search,page);}
 @PostMapping public DonationView finalize(@RequestBody FinalizeRequest request){return service.finalize(request);}
 @GetMapping public Page<DonationView> list(@RequestParam long organizationId,@RequestParam(required=false) Long unitId,@RequestParam(defaultValue="0") int page){return service.list(organizationId,unitId,page);}
 @GetMapping("/{id}") public DonationView get(@PathVariable long id){return service.get(id);}
}
