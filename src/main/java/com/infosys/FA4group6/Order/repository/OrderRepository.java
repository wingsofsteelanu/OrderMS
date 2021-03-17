package com.infosys.FA4group6.Order.repository;

import org.springframework.data.repository.CrudRepository;

import com.infosys.FA4group6.Order.entity.OrderDetails;

public interface OrderRepository extends CrudRepository<OrderDetails, Integer>{

}
