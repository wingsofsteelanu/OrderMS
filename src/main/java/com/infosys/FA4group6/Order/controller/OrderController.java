package com.infosys.FA4group6.Order.controller;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

import com.infosys.FA4group6.Order.entity.ProductsOrdered;
import com.infosys.FA4group6.Order.services.OrderService;
import com.infosys.FA4group6.OrderDTO.OrderDetailsDTO;
import com.infosys.FA4group6.OrderDTO.ProductId;
import com.infosys.FA4group6.OrderDTO.ProductsOrderedDTO;

@RequestMapping("/api")
@RestController
public class OrderController {
	
	@Autowired
	private OrderService orderService;
	@Autowired
	public RestTemplate restTemplate;

	public String cartURL;

	public String productURL;
	
//GET Request using buyerId******************************************************************************************************
	@GetMapping("/orders/{buyerId}")
	public ArrayList <OrderDetailsDTO> getOrders(@PathVariable("buyerId") Integer buyerId ) {
		return orderService.getAllOrders(buyerId);	
		}
	
//GET Request with sellerId******************************************************************************************************
	@GetMapping("/orders/seller/{sellerId}")
	public ArrayList<ProductsOrdered> getSellerOrders(@PathVariable("sellerId") Integer sellerId) {
			return orderService.getSellerOrders(sellerId);	
		}	
	
//PUT Request******************************************************************************************************************** 
	@PutMapping("/orders/seller/status")
	public String updateStatus (@RequestBody ProductsOrderedDTO productsOrderedDTO) {
		Integer orderId=productsOrderedDTO.getOrderId();
		Integer prodId=productsOrderedDTO.getProdId();
		String status= productsOrderedDTO.getStatus();
		return orderService.updateStatus(orderId,prodId,status);
	}
	
//DELETE Request using orderId***************************************************************************************************
	@DeleteMapping("/orders/cancel/{orderId}")
	public String cancelOrder(@PathVariable("orderId") Integer orderId ) {
		return orderService.cancelAnOrder(orderId);
	}
	
//POST Request reOrdering using orderId and buyerId******************************************************************************
	@PostMapping("/orders/reOrder/{orderId}/{buyerId}")
	public String reorder(@PathVariable("orderId") Integer orderId,@PathVariable("buyerId") Integer buyerId) {
		ArrayList <OrderDetailsDTO> orders=orderService.getAllOrders(buyerId);
		Boolean request=false;
		String result="";
		for(int i =0; i<orders.size();i++) {
			if(orderId.equals(orders.get(i).getOrderId())) {
				OrderDetailsDTO order=orders.get(i);
				request=true;
				result=orderService.reOrder(order);
			}
		}
		if(request){
			return result;
		}else{
			return "Reorder is not successful";
		}
	}
	
	
//POST Request placing Order*****************************************************************************************************
	@PostMapping("/orders/placeOrder")
	public String placeOrder(@RequestBody OrderDetailsDTO order) {
		
		String cartURL1=cartURL+"cart/checkout/"+order.getBuyerId();		
		try {
        ResponseEntity<ProductsOrderedDTO[]> responseEntity = restTemplate.getForEntity(cartURL1, ProductsOrderedDTO[].class);   
        ProductsOrderedDTO[] objects = responseEntity.getBody();

		List<Integer> prodIds= new ArrayList<Integer>();
		for(int i=0;i<objects.length;i++) {prodIds.add(objects[i].getProdId());}
		
		ProductId prodId1=new ProductId();
		prodId1.setProdId(prodIds);

		
		ResponseEntity<ProductsOrderedDTO[]> products1 = restTemplate.getForEntity(productURL, ProductsOrderedDTO[].class);
		ProductsOrderedDTO[] products12 = products1.getBody();
		ArrayList<ProductsOrderedDTO> productsOrderedDTOs=new ArrayList<ProductsOrderedDTO>();
		for(int i=0;i<products12.length;i++) {
			productsOrderedDTOs.add(products12[i]);
		}
		productsOrderedDTOs.forEach(element->{
			for(int j=0;j<objects.length;j++) {
				if(objects[j].getProdId().equals(element.getProdId())) {
					element.setQuantity(objects[j].getQuantity());
				}
			}
		});
		order.setOrderedProducts(productsOrderedDTOs);
		
		orderService.placeOrder(order);
		}
		catch(Exception e){
			e.printStackTrace();
			return "Error occured while placing the order! ";
		}
		return "Order Sucessfully";
	}
	
	
	
	
}
