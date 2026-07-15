package com.got.retailpos.customers.infrastructure;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.got.retailpos.customers.domain.Customer;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

	boolean existsByNormalizedPhone(String normalizedPhone);

	boolean existsByNormalizedPhoneAndIdNot(String normalizedPhone, UUID id);

	@Query("""
			select customer from Customer customer
			where customer.active = true
			  and (lower(customer.name) like lower(concat('%', :search, '%'))
			       or lower(coalesce(customer.phone, '')) like lower(concat('%', :search, '%'))
			       or lower(coalesce(customer.normalizedPhone, '')) like lower(concat('%', :search, '%')))
			""")
	Page<Customer> search(@Param("search") String search, Pageable pageable);

	Page<Customer> findAllByActiveTrue(Pageable pageable);
}
