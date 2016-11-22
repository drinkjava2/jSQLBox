package test.config;

import java.util.Properties;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import com.github.drinkjava2.BeanBox;
import com.github.drinkjava2.jsqlbox.SqlBoxContext;
import com.mchange.v2.c3p0.ComboPooledDataSource;

/**
 * This Java class is a configuration file, equal to XML in Spring, see jBeanBox
 * project
 *
 */
public class Config {
	// jSqlBox & jBeanBox initialize
	public static void initialize() {
		SqlBoxContext.DEFAULT_SQLBOX_CONTEXT.setDataSource((DataSource) BeanBox.getBean(DSPoolBeanBox.class));
		SqlBoxContext.DEFAULT_SQLBOX_CONTEXT.setShowSql(false);
		BeanBox.defaultContext.setAOPAround("test.\\w*.\\w*", "tx_\\w*", new TxInterceptorBox(), "invoke");
	}

	// MySql connection URL
	static class MySqlConfigBox extends BeanBox {
		{
			setProperty("jdbcUrl", "jdbc:mysql://127.0.0.1:3306/test?rewriteBatchedStatements=true&useSSL=false");
			setProperty("driverClass", "com.mysql.jdbc.Driver");
		}
	}

	// Oracle connection URL
	static class OracleConfigBox extends BeanBox {
		{
			setProperty("jdbcUrl", "jdbc:oracle:thin:@127.0.0.1:1521:xe");
			setProperty("driverClass", "oracle.jdbc.OracleDriver");
		}
	}

	// Data source pool setting
	static class DSPoolBeanBox extends MySqlConfigBox {
		{
			setClassOrValue(ComboPooledDataSource.class);
			setProperty("user", "root");// set to your user
			setProperty("password", "root888");// set to your password
			setProperty("minPoolSize", 4);
			setProperty("maxPoolSize", 30);
			setProperty("CheckoutTimeout", 5000);
		}
	}

	// Spring TxManager
	static class TxManagerBox extends BeanBox {
		{
			setClassOrValue(DataSourceTransactionManager.class);
			setProperty("dataSource", DSPoolBeanBox.class);
		}
	}

	// Spring TransactionInterceptor
	static class TxInterceptorBox extends BeanBox {
		{
			Properties props = new Properties();
			props.put("tx_*", "PROPAGATION_REQUIRED");
			setConstructor(TransactionInterceptor.class, TxManagerBox.class, props);
		}
	}

	public static class JdbcTemplateBox extends BeanBox {
		{
			setConstructor(JdbcTemplate.class, DSPoolBeanBox.class);
		}
	}

}