package com.comandos.rest.users;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.comandos.model.User;

public class UserFormRequest {
	
	private Long id;
	private LocalDateTime creationDate;
	private String name;
	private String cpf;
	private LocalDate birth;
	private String address;
	private String email;
	private String phone;
	
	// --- CONSTRUCTORS ---

	public UserFormRequest() {
		super();
	}

	public UserFormRequest(String name, String cpf, LocalDate birth, String address, String email, String phone) {
		super();
		this.name = name;
		this.cpf = cpf;
		this.birth = birth;
		this.address = address;
		this.email = email;
		this.phone = phone;
	}

	public UserFormRequest(Long id, LocalDateTime creationDate, String name, String cpf, LocalDate birth,
			String address, String email, String phone) {
		super();
		this.id = id;
		this.creationDate = creationDate;
		this.name = name;
		this.cpf = cpf;
		this.birth = birth;
		this.address = address;
		this.email = email;
		this.phone = phone;
	}

	// --- GETTERS AND SETTERS ---
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public LocalDateTime getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(LocalDateTime creationDate) {
		this.creationDate = creationDate;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCpf() {
		return cpf;
	}

	public void setCpf(String cpf) {
		this.cpf = cpf;
	}

	public LocalDate getBirth() {
		return birth;
	}

	public void setBirth(LocalDate birth) {
		this.birth = birth;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	// --- TOSTRING ---
	
	@Override
	public String toString() {
		return "UserFormRequest [id=" + id + ", creationDate=" + creationDate + ", name=" + name + ", cpf=" + cpf
				+ ", birth=" + birth + ", address=" + address + ", email=" + email + ", phone=" + phone + "]";
	}
	
	// --- MODEL MAPPING CONVERSIONS ---

	/**
	 * Converts the DTO request into a clean database entity model.
	 * Explicitly uses setters to block parameter position mix-ups.
	 * @return An absolute User entity model instance
	 */
	public User toModel() {
		User user = new User();
		user.setId(this.id);
		user.setCreationDate(this.creationDate);
		user.setName(this.name);
		user.setCpf(this.cpf);
		user.setBirth(this.birth);
		user.setAddress(this.address);
		user.setPhone(this.phone);
		user.setEmail(this.email);
		return user;
	}
	
	/**
	 * Converts a database entity user model back into a sanitized API request DTO response.
	 * @param user The raw entity instance fetched from the database repository
	 * @return A new UserFormRequest DTO wrapper instance
	 */
	public static UserFormRequest fromModel( User user ) {
		UserFormRequest request = new UserFormRequest();
		request.setId(user.getId());
		request.setCreationDate(user.getCreationDate());
		request.setName(user.getName());
		request.setCpf(user.getCpf());
		request.setBirth(user.getBirth());
		request.setAddress(user.getAddress());
		request.setPhone(user.getPhone());
		request.setEmail(user.getEmail());
		return request;
	}
}
