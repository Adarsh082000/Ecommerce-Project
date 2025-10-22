package com.ecom.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.ecom.model.Category;
import com.ecom.model.Product;
import com.ecom.model.ProductOrder;
import com.ecom.model.UserDtls;
import com.ecom.service.CartService;
import com.ecom.service.CategoryService;
import com.ecom.service.OrderService;
import com.ecom.service.ProductService;
import com.ecom.service.UserService;
import com.ecom.util.CommonUtil;
import com.ecom.util.OrderStatus;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {

	@Autowired
	private CategoryService categoryService;
	
	@Autowired
	private ProductService productService;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private CartService cartService;
	
	@Autowired
	private OrderService orderService;
	
	@Autowired
	private CommonUtil commonUtil;
	
	@Autowired
	private PasswordEncoder passwordEncoder;


	@GetMapping("/")
	public String index() {
		return "admin/index";
	}
	
	@ModelAttribute
	public void getUserDetails(Principal p, Model m) {
		
		if(p!=null) {
			String email = p.getName();
			UserDtls userByEmail = userService.getUserByEmail(email);
			m.addAttribute("user", userByEmail);
			Integer countCart = cartService.getCountCart(userByEmail.getId());
			m.addAttribute("countCart", countCart);
		}
		
		List<Category> allActiveCategory = categoryService.getAllActiveCategory();
		m.addAttribute("categorys", allActiveCategory);
		
	}

	@GetMapping("/loadAddProduct")
	public String loadAddProduct(Model m) {
		List<Category> categories = categoryService.getAllCategory();
		m.addAttribute("categories", categories);
		return "admin/addProduct";
	}

	@GetMapping("/category")
	public String category(Model m, @RequestParam(defaultValue = "0") Integer pageNo, 
			@RequestParam(defaultValue = "10") Integer pageSize) {
		/* m.addAttribute("categories", categoryService.getAllCategory()); */
		
		Page<Category> page = categoryService.getAllCategoryPagination(pageNo, pageSize);
		List<Category> categories = page.getContent();
		
		m.addAttribute("categories", categories);
		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());
		
		return "admin/category";
	}

	@PostMapping("/saveCategory")
	public String saveCategory(@ModelAttribute Category category, @RequestParam() MultipartFile file,
			HttpSession session) throws IOException {

		String imageName = file.isEmpty() ? "default.jpg" : file.getOriginalFilename();
		category.setImageName(imageName);

		Boolean existCategory = categoryService.existCategory(category.getName());

		if (existCategory) {
			session.setAttribute("errorMsg", "Category Name already exists");
		} else {

			Category saveCategory = categoryService.saveCategory(category);

			if (ObjectUtils.isEmpty(saveCategory)) {
				session.setAttribute("errorMsg", "Not saved ! internal server error");
			} else {

				File saveFile = new ClassPathResource("static/img").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "category_img" + File.separator
						+ file.getOriginalFilename());

				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

				session.setAttribute("succMsg", "Saved successfully");
			}
		}

		return "redirect:/admin/category";
	}

	@GetMapping("/deleteCategory/{id}")
	public String deleteCategory(@PathVariable int id, HttpSession session) {
		Boolean deleteCategory = categoryService.deleteCategory(id);

		if (deleteCategory) {
			session.setAttribute("succMsg", "category deleted successfully");
		} else {
			session.setAttribute("errorMsg", "something went wrong on server");
		}

		return "redirect:/admin/category";
	}

	@GetMapping("loadEditCategory/{id}")
	public String loadEditCategory(@PathVariable int id, Model m) {

		m.addAttribute("category", categoryService.getCategoryById(id));

		return "admin/edit_category";
	}

	@PostMapping("/updateCategory")
	public String updateCategory(@ModelAttribute Category category, @RequestParam MultipartFile file,
			HttpSession session) throws IOException {

		Category oldCategory = categoryService.getCategoryById(category.getId());
		String imageName = file.isEmpty() ? oldCategory.getImageName() : file.getOriginalFilename();

		if (!ObjectUtils.isEmpty(oldCategory)) {
			oldCategory.setName(category.getName());
			oldCategory.setActive(category.isActive());
			oldCategory.setImageName(imageName);
		}

		Category updateCategory = categoryService.saveCategory(oldCategory);

		if (!ObjectUtils.isEmpty(updateCategory)) {
			if (!file.isEmpty()) {

				File saveFile = new ClassPathResource("static/img").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "category_img" + File.separator
						+ file.getOriginalFilename());

				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			}
			session.setAttribute("succMsg", "Category update success");
		} else {
			session.setAttribute("errorMsg", "Something went wrong on server");

		}

		return "redirect:/admin/loadEditCategory/" + category.getId();
	}
	
	@PostMapping("/saveProduct")
	public String saveProduct(@ModelAttribute Product product, @RequestParam("file") MultipartFile image, HttpSession session) throws IOException {
		
		String name = image.isEmpty() ? "default.jpg" : image.getOriginalFilename();
		product.setImage(name);	
		product.setDiscount(0);
		product.setDiscountPrice(product.getPrice());
		Product saveProduct = productService.saveProduct(product);
		
		if(!ObjectUtils.isEmpty(saveProduct)) {
			if(!image.isEmpty()) {
				
				File saveFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "product_img" + File.separator + name);
				Files.copy(image.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			}
			session.setAttribute("succMsg", "Product updated successfully");
		}else {
			session.setAttribute("errorMsg", "Something wrong on server");
		}
		
		return "redirect:/admin/loadAddProduct";
		
	}
	
	@GetMapping("/products")
	public String loadViewProduct(Model m, @RequestParam(defaultValue = "") String ch,
			@RequestParam(defaultValue = "0") Integer pageNo,
			@RequestParam(defaultValue = "10") Integer pageSize) {
		/*
		 * List<Product> products = null; if(ch != null && ch.length() > 0) { products =
		 * productService.searchProduct(ch); }else { products =
		 * productService.getAllProducts(); } m.addAttribute("products", products);
		 */
		
		Page<Product> page = null;
		if (ch != null && ch.length() > 0) {
			page = productService.searchProductPagination(pageNo, pageSize, ch);
		} else {
			page = productService.getAllProductsPagination(pageNo, pageSize);
		}
		m.addAttribute("products", page.getContent());

		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());
		
		return "admin/products";
		
	}
	
	@GetMapping("/deleteProduct/{id}")
	public String deleteProduct(@PathVariable int id, HttpSession session) {
		
		Boolean deleteProduct = productService.deleteProduct(id);
		
		if(deleteProduct) {
			session.setAttribute("succMsg", "Product deleted successfully");
		}else {
			session.setAttribute("errorMsg", "Something wrong on server");
		}
		
		return "redirect:/admin/products";
	}
	
	@GetMapping("/editProduct/{id}")
	public String editProduct(@PathVariable int id, Model m) {
		m.addAttribute("product", productService.getProductById(id));
		m.addAttribute("categories", categoryService.getAllCategory());
		return "admin/edit_product";
		
	}
	
	@PostMapping("updateProduct")
	public String updateProduct(@ModelAttribute Product product, @RequestParam("file") MultipartFile image, HttpSession session, Model m) {
		
		if(product.getDiscount()<0 || product.getDiscount()>100) {
			session.setAttribute("errorMsg", "Invalid Discount");
		}else {
			Product updatedProduct = productService.updateProduct(product, image);
			if(!ObjectUtils.isEmpty(updatedProduct)) {
				session.setAttribute("succMsg", "Product updated successfully");
			}else {
				session.setAttribute("errorMsg", "Something wrong on server");
			}
		}
		return "redirect:/admin/editProduct/" + product.getId();
	}
	
	
	@GetMapping("/users")
	public String getAllUsers(Model m, @RequestParam Integer type) {
		
		List<UserDtls> users = null;
		if(type == 1) {
			users = userService.getUsers("ROLE_USER");
		}else {
			users = userService.getUsers("ROLE_ADMIN");
		}
		m.addAttribute("userType", type); 
		m.addAttribute("users", users);
		return "admin/users";
	}
	
	@GetMapping("/updateSts")
	public String updateUserAccountStatus(@RequestParam Boolean status, @RequestParam Integer id, @RequestParam Integer type,
			HttpSession session) {
		
		Boolean accountStatus = userService.updateAccountStatus(id, status);
		if(accountStatus) {
			session.setAttribute("SuccMsg", "Account status updated");
		}else {
			session.setAttribute("errorMsg", "Something went wrong on server");
		}
		return "redirect:/admin/users?type="+type;
	}

	
	@GetMapping("/orders")
	public String getAllOrders(Model m, @RequestParam(defaultValue = "0") Integer pageNo,
			@RequestParam(defaultValue = "10") Integer pageSize) {
		
		/*
		 * List<ProductOrder> allOrders = orderService.getAllOrders();
		 * m.addAttribute("orders", allOrders); m.addAttribute("srch", false);
		 */
		
		Page<ProductOrder> page = orderService.getAllOrdersPagination(pageNo, pageSize);
		m.addAttribute("orders", page.getContent());
		m.addAttribute("srch", false);
		
		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());
		
		return "admin/orders";
	}
	
	@PostMapping("/update-order-status")
	public String updateOrderStatus(@RequestParam Integer id, @RequestParam Integer st, HttpSession session) throws Exception {
		
		
		OrderStatus[] values = OrderStatus.values();
		String status = null;
		
		for (OrderStatus orderStatus: values) {
			if(orderStatus.getId().equals(st)) {
				status = orderStatus.getName();
			}
		}
		
		ProductOrder updateOrder = orderService.updateOrderStatus(id, status);
		
		commonUtil.sendMailForProductOrder(updateOrder, status);
		
		if(!ObjectUtils.isEmpty(updateOrder)) {
			session.setAttribute("succMsg", "Status Updated");
		}else {
			session.setAttribute("errorMsg", "status not updated");
		}
		
		return "redirect:/admin/orders";
	}
	
	
	
	@GetMapping("/search-order")
	public String searchProduct(@RequestParam String orderId, Model m, HttpSession session, 
			@RequestParam(defaultValue = "0") Integer pageNo,
			@RequestParam(defaultValue = "10") Integer pageSize) {
		
		if(orderId != null && orderId.length()>0) {
			ProductOrder order = orderService.getOrdersByOrderId(orderId.trim());
			
			if(ObjectUtils.isEmpty(order)) {
				session.setAttribute("errorMsg", "Incorrect orderId");
				m.addAttribute("orderDtls", null);
			}else {
				m.addAttribute("orderDtls", order);
			}
			m.addAttribute("srch", true);
		}else {
			/*
			 * List<ProductOrder> allOrders = orderService.getAllOrders();
			 * m.addAttribute("orders", allOrders); m.addAttribute("srch", false);
			 */
			
			Page<ProductOrder> page = orderService.getAllOrdersPagination(pageNo, pageSize);
			m.addAttribute("orders", page);
			m.addAttribute("srch", false);

			m.addAttribute("pageNo", page.getNumber());
			m.addAttribute("pageSize", pageSize);
			m.addAttribute("totalElements", page.getTotalElements());
			m.addAttribute("totalPages", page.getTotalPages());
			m.addAttribute("isFirst", page.isFirst());
			m.addAttribute("isLast", page.isLast());
		}
		return "/admin/orders";
	}
	
	@GetMapping("/add-admin")
	public String loadAddAdmin() {
		return "admin/add_admin";
	}
	
	
	@PostMapping("/save-admin")
	public String saveAdmin(@ModelAttribute UserDtls user, @RequestParam MultipartFile file, HttpSession session) throws IOException {
		
		String imageName = file.isEmpty() ? "default.jpg" : file.getOriginalFilename();
		user.setProfileImage(imageName);
		UserDtls saveUser = userService.saveAdmin(user);
		
		if (!ObjectUtils.isEmpty(saveUser)) {
			if (!file.isEmpty()) {
				File saveFile = new ClassPathResource("static/img").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "profile_img" + File.separator
						+ file.getOriginalFilename());

//				System.out.println(path);
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			}
			session.setAttribute("succMsg", "Register successfully");
		} else {
			session.setAttribute("errorMsg", "something wrong on server");
		}
		
		return "redirect:/admin/add-admin";
	}
	
	@GetMapping("/profile")
	public String profile() {
		return "/admin/profile";
	}
	
	@PostMapping("/update-profile")
	public String updateProfile(@ModelAttribute UserDtls user, @RequestParam MultipartFile file, HttpSession session) {
		
		UserDtls updateUserProfile = userService.updateUserProfile(user, file);
		if(ObjectUtils.isEmpty(updateUserProfile)) {
			session.setAttribute("errorMsg", "Profile not updated");
		}else {
			session.setAttribute("succMsg", "Profile Updated");
		}
		return "redirect:/admin/profile";
	}
	
	
	
	@PostMapping("/change-password")
	public String changePassword(@RequestParam String newPassword, @RequestParam String currentPassword, Principal p,
			HttpSession session) {
		
		UserDtls loggedInUserDetails = commonUtil.getLoggedInUserDetails(p);
		
		boolean matches = passwordEncoder.matches(currentPassword, loggedInUserDetails.getPassword());
		
		if (matches) {
			String encodePassword = passwordEncoder.encode(newPassword);
			loggedInUserDetails.setPassword(encodePassword);
			UserDtls updateUser = userService.updateUser(loggedInUserDetails);
			if (ObjectUtils.isEmpty(updateUser)) {
				session.setAttribute("errorMsg", "Password not updated !! Error in server");
			} else {
				session.setAttribute("succMsg", "Password Updated sucessfully");
			}
		} else {
			session.setAttribute("errorMsg", "Current Password incorrect");
		}
		
		
		return "redirect:/admin/profile";
		
	}
}
