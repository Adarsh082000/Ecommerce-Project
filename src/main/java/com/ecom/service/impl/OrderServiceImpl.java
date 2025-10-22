package com.ecom.service.impl;


import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ecom.model.Cart;
import com.ecom.model.OrderAddress;
import com.ecom.model.OrderRequest;
import com.ecom.model.ProductOrder;
import com.ecom.repository.CartRepository;
import com.ecom.repository.ProductOrderRepository;
import com.ecom.service.OrderService;
import com.ecom.util.CommonUtil;
import com.ecom.util.OrderStatus;

@Service
public class OrderServiceImpl implements OrderService{
	
	@Autowired
	private CartRepository cartRepository;
	
	@Autowired
	private ProductOrderRepository productOrderRepository;
	
	@Autowired
	private CommonUtil commonUtil;

	@Override
	public void saveOrder(Integer userId, OrderRequest orderRequest) throws Exception {
		
		List<Cart> carts = cartRepository.findByUserId(userId);
		
		for (Cart cart: carts) {
			
			ProductOrder order = new ProductOrder();
			
			order.setOrderId(UUID.randomUUID().toString());
			order.setOrderDate(LocalDate.now());
			order.setProduct(cart.getProduct());
			order.setPrice(cart.getProduct().getDiscountPrice());
			order.setQuantity(cart.getQuantity());
			order.setUser(cart.getUser());
			order.setStatus(OrderStatus.IN_PROGRESS.getName());
			order.setPaymentType(orderRequest.getPaymentType());
			
			OrderAddress address = new OrderAddress();
			
			address.setFirstName(orderRequest.getFirstName());
			address.setLastName(orderRequest.getLastName());
			address.setEmail(orderRequest.getEmail());
			address.setMobileNo(orderRequest.getMobileNo());
			address.setAddress(orderRequest.getAddress());
			address.setCity(orderRequest.getCity());
			address.setState(orderRequest.getState());
			address.setPincode(orderRequest.getPincode());
			
			order.setOrderAddress(address);
			
			ProductOrder saveOrder = productOrderRepository.save(order);
			commonUtil.sendMailForProductOrder(saveOrder, "success");
		}
		
		
	}



	@Override
	public List<ProductOrder> getOrdersByUser(Integer userId) {
		
		List<ProductOrder> orders = productOrderRepository.findByUserId(userId);
		return orders;
	}

	@Override
	public ProductOrder updateOrderStatus(Integer id, String status) {
		Optional<ProductOrder> findById = productOrderRepository.findById(id);
		if(findById.isPresent()) {
			ProductOrder productOrder = findById.get();
			productOrder.setStatus(status);
			productOrderRepository.save(productOrder);
			return productOrder;
		}
		return null;
	}

	@Override
	public List<ProductOrder> getAllOrders() {
		return productOrderRepository.findAll();
	}
	
	@Override
	public ProductOrder getOrdersByOrderId(String orderId) {
		return productOrderRepository.findByOrderId(orderId);
	}



	@Override
	public Page<ProductOrder> getAllOrdersPagination(Integer pageNo, Integer pageSize) {
		Pageable pageable = PageRequest.of(pageNo, pageSize);
		return productOrderRepository.findAll(pageable);
	}

}
