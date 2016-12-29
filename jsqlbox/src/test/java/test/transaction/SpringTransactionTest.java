package test.transaction;

import static com.github.drinkjava2.jsqlbox.SqlHelper.empty;
import static com.github.drinkjava2.jsqlbox.SqlHelper.questionMarks;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.github.drinkjava2.jsqlbox.Dao;
import com.github.drinkjava2.jsqlbox.SqlBoxContext;

import test.config.SpringConfig;
import test.config.TestPrepare;
import test.config.po.User;

/**
 * This is to test use Spring's Declarative Transaction
 *
 * @author Yong Zhu
 *
 * @version 1.0.0
 * @since 1.0.0
 */
@Transactional(propagation = Propagation.REQUIRED)
public class SpringTransactionTest {

	public void tx_InsertUser1() {
		User u = new User();
		Dao.execute("insert into ", u.table(), //
				" (", u.userName(), empty("user1"), //
				", ", u.address(), empty("address1"), //
				", ", u.age(), ")", empty("10"), //
				questionMarks());
	}

	public void tx_InsertUser2() {
		User u = new User();
		Dao.execute("insert into ", u.table(), //
				" (", u.userName(), empty("user2"), //
				", ", u.address(), empty("address2"), //
				", ", u.age(), ")", empty("20"), //
				questionMarks());
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void tx_doInsert() {
		User u = new User();
		tx_InsertUser1();
		int i = Dao.queryForInteger("select count(*) from ", u.table());
		Assert.assertEquals(1, i);
		System.out.println(i / 0);// throw a runtime exception
		tx_InsertUser2();
	}

	@Test
	public void doTest() {
		TestPrepare.prepareDatasource_SetDefaultSqlBoxConetxt_RecreateTables();
		TestPrepare.closeDatasource_CloseDefaultSqlBoxConetxt();

		AnnotationConfigApplicationContext springCTX = new AnnotationConfigApplicationContext(SpringConfig.class);
		SqlBoxContext.setDefaultSqlBoxContext(springCTX.getBean(SqlBoxContext.class));
		SpringTransactionTest tester = springCTX.getBean(SpringTransactionTest.class);
		boolean foundException = false;
		try {
			tester.tx_doInsert();
		} catch (Exception e) {
			foundException = true;
			User u = new User();
			int i = Dao.queryForInteger("select count(*) from ", u.table());
			Assert.assertEquals(0, i);
		}
		Assert.assertEquals(foundException, true);

		SqlBoxContext.getDefaultSqlBoxContext().close();
		springCTX.close();
	}

}