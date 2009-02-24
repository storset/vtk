package org.vortikal.repo2;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.config.MethodInvokingFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

public class TransactionalMethodInvokingFactoryBean extends MethodInvokingFactoryBean {

    private PlatformTransactionManager transactionManager;
    
    public void afterPropertiesSet() throws Exception {
        TransactionStatus transaction = this.transactionManager.getTransaction(null);
        try {
            super.afterPropertiesSet();
            this.transactionManager.commit(transaction);
        } catch (Exception e) {
            this.transactionManager.rollback(transaction);
        }
    }

    @Required public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }
}
