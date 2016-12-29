package test;

import org.junit.Assert;

import com.github.drinkjava2.BeanBox;
import com.github.drinkjava2.jsqlbox.Dao;
import com.github.drinkjava2.jsqlbox.IEntity;
import com.github.drinkjava2.jsqlbox.SqlBoxContext;
import com.github.drinkjava2.jsqlbox.id.UUIDGenerator;

import test.config.JBeanBoxConfig.DefaultSqlBoxContextBox;

public class HelloWorld2 {

	public static class User implements IEntity {
		Integer id;
		String userName;
		String address;
		String phoneNumber;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getUserName() {
			return userName;
		}

		public void setUserName(String userName) {
			this.userName = userName;
		}

		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}

		public String getPhoneNumber() {
			return phoneNumber;
		}

		public void setPhoneNumber(String phoneNumber) {
			this.phoneNumber = phoneNumber;
		}

	}

	public static void main(String[] args) {
		SqlBoxContext.setDefaultSqlBoxContext(BeanBox.getBean(DefaultSqlBoxContextBox.class));

		User user = new User();
		Dao.executeQuiet("delete from ", user.table());
		user.setUserName("Sam");
		user.insert();
		Assert.assertEquals("Sam", Dao.queryForString("select USERNAME from users"));

		user.box().configTable("users2");
		user.box().configIdGenerator("phoneNumber", UUIDGenerator.INSTANCE);
		user.box().configColumnName("userName", "address");
		Dao.executeQuiet("delete from ", user.table());
		user.insert();
		Assert.assertEquals("Sam", Dao.queryForString("select ADDRESS from users2"));
		Assert.assertEquals(32, Dao.queryForString("select PHONE_NUMBER from users2").length());
	}

}