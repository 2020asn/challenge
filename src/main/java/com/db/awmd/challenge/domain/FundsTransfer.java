package com.db.awmd.challenge.domain;

import java.math.BigDecimal;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FundsTransfer {

	@NotNull
	@NotEmpty
	private String accountFrom;

	@NotNull
	@NotEmpty
	private String accountTo;

	@NotNull
	@Min(value = 0, message = "Transfer amount must be positive.")
	private BigDecimal amount;

}
