package com.infosys.FA4group6.Order.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.infosys.FA4group6.Order.entity.OrderDetails;
import com.infosys.FA4group6.Order.entity.ProductsOrdered;
import com.infosys.FA4group6.Order.repository.OrderRepository;
import com.infosys.FA4group6.Order.repository.ProductsOrderedRepository;
import com.infosys.FA4group6.OrderDTO.OrderDetailsDTO;
import com.infosys.FA4group6.OrderDTO.ProductsOrderedDTO;




@Service
public class OrderService {
	@Autowired
	OrderRepository orderRepo;
	@Autowired
	OrderDetails orderDetails;
	@Autowired
	ProductsOrderedRepository orderProdsRepo;
	@Autowired
	ProductsOrdered productsOrdered;
	@Autowired
	public RestTemplate restTemplate;

	public String userURL;

//getAllorders using buyerId***************************************************************************************
	public ArrayList <OrderDetailsDTO> getAllOrders(Integer buyerId) {
        Iterable<OrderDetails> ordersdetails=orderRepo.findAll();
        ArrayList <OrderDetailsDTO> orders= new ArrayList<>();
        List<ProductsOrdered> allOrderedProducts=(List<ProductsOrdered>) orderProdsRepo.findAll();
        for(OrderDetails order: ordersdetails){
            if (order.getBuyerId().equals(buyerId)) {
				ArrayList<ProductsOrderedDTO> orderedProducts=new ArrayList<>();
                for(int i=0;i<allOrderedProducts.size();i++){
                    ProductsOrdered orderprod=allOrderedProducts.get(i);
					// Finding the ordered Products for each and every order
                    if(order.getOrderId().equals(orderprod.getOrderId())) {
                        ProductsOrderedDTO prod=new ProductsOrderedDTO();
                        BeanUtils.copyProperties(orderprod, prod);
                        orderedProducts.add(prod);
                    }
                }
                OrderDetailsDTO ob = new OrderDetailsDTO();
                BeanUtils.copyProperties(order, ob);
                ob.setOrderedProducts(orderedProducts);
                orders.add(ob);
                }
        }
        return orders;
    }	
	
//getSellerOrder using sellerId************************************************************************************
    public ArrayList<ProductsOrdered> getSellerOrders(Integer sellerId) {
		ArrayList <ProductsOrdered> orderedProducts = new ArrayList<>();
		orderProdsRepo.findAll().forEach((ProductsOrdered ordProd)->{
			if(ordProd.getSellerid().equals(sellerId)) {
				orderedProducts.add(ordProd);
			}
		});
		
		return orderedProducts;
	}
	
//Update Status****************************************************************************************************
  	public String updateStatus(Integer orderId,Integer prodId,String status) {
  		Boolean flag=false;
  		try {
  		List<ProductsOrdered> products=(List<ProductsOrdered>) orderProdsRepo.findAll();
  		for(int i=0;i<products.size();i++){
  			ProductsOrdered product=products.get(i);
  			// Checking the orderId and ProdId before updating
  			if(product.getOrderId().equals(orderId) && product.getProdId().equals(prodId)) {
  				BeanUtils.copyProperties(product,productsOrdered);
  				orderProdsRepo.delete(product);
  				productsOrdered.setStatus(status);
  				orderProdsRepo.save(productsOrdered);
  				flag=true;
  			}
  		}
  		}catch(Exception e){
  			e.printStackTrace();
  			return "Error in updating the order! Contact your Admin";
  		}
  		if(flag){
  			return "Order status updated successfully";
  		}else{
  			return "Updation is not successful. Check for issues";
  		}
  	}
  	
  	
//placing order****************************************************************************************
	public void placeOrder(OrderDetailsDTO order) {
		ArrayList<ProductsOrderedDTO> productsReceived=(ArrayList<ProductsOrderedDTO>) order.getOrderedProducts();
		
		BigDecimal amount=new BigDecimal(0);
		for (int j=0;j<productsReceived.size();j++) {
			ProductsOrderedDTO productsOrderedDTO=productsReceived.get(j);
			amount=amount.add(productsOrderedDTO.getPrice().multiply(new BigDecimal(productsOrderedDTO.getQuantity())));
		}
		Integer eligibleDiscount=new Integer(amount.multiply(new BigDecimal(0.1)).intValue());
		
      
        Integer [] valuesArray=this.usingRewardPoints(order.getBuyerId(),eligibleDiscount);
        BigDecimal discount=new BigDecimal(valuesArray[0]);


		String privilegedURL=userURL+"buyer/isPrivilege/"+order.getBuyerId();
	
		ResponseEntity<Boolean> responseEntity1 = restTemplate.getForEntity(privilegedURL, Boolean.class);
		
		Boolean isPrivileged=responseEntity1.getBody();
		
		BigDecimal shippingCost=new BigDecimal(50);
		if(isPrivileged.equals(true)) {
			shippingCost=new BigDecimal(0);
		}
		amount=amount.subtract(discount);
		amount=amount.add(shippingCost);
		order.setAmount(amount);
		order.setDate(new Date());order.setStatus("ORDER PLACED");
		
		BeanUtils.copyProperties(order, orderDetails);
		orderRepo.save(orderDetails);
						
		Integer orderId=orderDetails.getOrderId();
		productsReceived.forEach((ProductsOrderedDTO prod)->{
			prod.setOrderId(orderId);
			prod.setStatus("ORDER PLACED");
			BeanUtils.copyProperties(prod, productsOrdered);
			orderProdsRepo.save(productsOrdered);
		});
		
		// Calculating and Updating the reward points in the user service
		Integer newRewardPoints = new Integer(amount.intValue()/100)+valuesArray[1]; 
		String updateRewardPointsUrl = userURL +"rewardPoint/update/"+order.getBuyerId()+"/"+newRewardPoints;
		restTemplate.put(updateRewardPointsUrl,newRewardPoints,Integer.class);
	}
	
//reOrder  ********************************************************************************************************
	public String reOrder(OrderDetailsDTO order) {
		try {
		ArrayList<ProductsOrderedDTO> orderedProducts=new ArrayList<ProductsOrderedDTO>();
		Integer orderId=order.getOrderId();
		
		// Finding all the products for that particular Order
		orderProdsRepo.findAll().forEach((product)->{
			if(product.getOrderId().equals(orderId)) {
				ProductsOrderedDTO prod=new ProductsOrderedDTO();
				BeanUtils.copyProperties(product, prod);
				orderedProducts.add(prod);}
		});
		
		order.setOrderedProducts(orderedProducts);
		order.setAmount(new BigDecimal(0));
		order.setOrderId(null);
		this.placeOrder(order);
		}catch(Exception excp){
			excp.printStackTrace();
			return "Error in placing the order! Contact your Admin";
		}
		return "Reorder is successful";
	}
	
	
//cancel an order using orderId****************************************************************************************
	public String cancelAnOrder(Integer orderId) {
		Boolean flag=false;
		Boolean flagOne=false;
		try{
			Iterable<OrderDetails> ordersEntities=orderRepo.findAll();
			// Deleting the order
			for(OrderDetails order: ordersEntities){
				if(order.getOrderId().equals(orderId)) {
					orderRepo.delete(order);					
					flagOne=true;
				}
			}
			List<ProductsOrdered> products=(List<ProductsOrdered>) orderProdsRepo.findAll();
			// Deleting all the product ordered in that order
			for(int i=0;i<products.size();i++){
				ProductsOrdered product=products.get(i);
				if(product.getOrderId().equals(orderId)) {
						orderProdsRepo.delete(product);
						flag=true;
				}
			}			
		}catch(Exception e){
			e.printStackTrace();
			return "Error in canceling the order!";
		}
		if(flag&flagOne){
			return "Order Canceled successfully.";
		}else{
			return "Cancellation is not successful";
		}
		
	}


//RewardPoints using buyerId & eligibleDiscount****************************************************************************************
	public Integer[] usingRewardPoints(Integer buyerId,Integer eligibleDiscount) {
      String rewardURL=userURL+"rewardPoint/"+buyerId;
      
      ResponseEntity<Integer> responseEntity = restTemplate.getForEntity(rewardURL, Integer.class);
     
      Integer reward=responseEntity.getBody();
      Integer discount=reward/4;
      if(discount>eligibleDiscount) {
          discount=eligibleDiscount;
          reward=reward-eligibleDiscount*4;
      }else {
          reward=0;
      }
      Integer [] valuesArray=new Integer[2];
      valuesArray[0]=discount;valuesArray[1]=reward;
      return valuesArray;
	}
}
