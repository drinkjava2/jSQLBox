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
 * This Java class is a configuration file, equal to XML in Spring, see jBeanBox project
 *
 */
public class JBeanBoxConfig {
	// jSqlBox & jBeanBox initialize
	public static void initialize() {
		SqlBoxContext.DEFAULT_SQLBOX_CONTEXT.setDataSource((DataSource) BeanBox.getBean(DataSourceBox.class));
		SqlBoxContext.DEFAULT_SQLBOX_CONTEXT.setShowSql(false);
		BeanBox.defaultContext.setAOPAround("test.\\w*.\\w*", "tx_\\w*", new TxInterceptorBox(), "invoke");
	}

	// Data source pool setting
	public static class C3P0Box extends BeanBox {
		{
			setClassOrValue(ComboPooledDataSource.class);
			setProperty("user", "root");// set to your user
			setProperty("password", "root888");// set to your password
			setProperty("minPoolSize", 4);
			setProperty("maxPoolSize", 30);
			setProperty("CheckoutTimeout", 5000);
		}
 
		// public ComboPooledDataSource create() {
		// ComboPooledDataSource ds = new ComboPooledDataSource();
		// ds.setUser("root");// set to your user
		// ds.setPassword("root888");// set to your password
		// ds.setMinPoolSize(4);
		// ds.setMaxPoolSize(30);
		// ds.setCheckoutTimeout(5000);
		// return ds;
		// }
	}

	// MySql connection URL
	static class MySqlDataSourceBox extends C3P0Box {
		{
			setProperty("jdbcUrl", "jdbc:mysql://127.0.0.1:3306/test?rewriteBatchedStatements=true&useSSL=false");
			setProperty("driverClass", "com.mysql.jdbc.Driver");
		}
	}

	// Oracle connection URL
	static class OracleDataSource extends C3P0Box {
		{
			setProperty("jdbcUrl", "jdbc:oracle:thin:@127.0.0.1:1521:xe");
			setProperty("driverClass", "oracle.jdbc.OracleDriver");
		}
	}

	// Data source pool setting
	public static class DataSourceBox extends MySqlDataSourceBox {
	}

	// CtxBox is a SqlBoxContent singleton
	public static class CtxBox extends BeanBox {
		{
			this.setConstructor(SqlBoxContext.class, DataSourceBox.class);
		}
	}

	// Spring TxManager
	static class TxManagerBox extends BeanBox {
		{
			setClassOrValue(DataSourceTransactionManager.class);
			setProperty("dataSource", DataSourceBox.class);
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
			setConstructor(JdbcTemplate.class, DataSourceBox.class);
		}
	}

}