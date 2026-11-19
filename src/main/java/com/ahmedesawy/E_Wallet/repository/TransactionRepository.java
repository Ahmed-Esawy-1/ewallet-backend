package com.ahmedesawy.E_Wallet.repository;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ahmedesawy.E_Wallet.entity.Transaction;
import com.ahmedesawy.E_Wallet.repository.projection.SuspiciousTransactionView;
import com.ahmedesawy.E_Wallet.repository.projection.TransactionHistoryView;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

	@Query("""
			SELECT  t.id AS id,
			        t.sourceWalletId AS sourceWalletId,
			        t.targetWalletId AS targetWalletId,
			        su.phone AS sourcePhone,
			        tu.phone AS targetPhone,
			        t.amount AS amount,
			        t.type As type,
			        t.status AS status,
			        t.createdAt AS createdAt
			FROM Transaction t
			LEFT JOIN Wallet as sw ON sw.id = t.sourceWalletId
			LEFT JOIN User as su ON su.id = sw.user.id
			LEFT JOIN Wallet as tw ON tw.id = t.targetWalletId
			LEFT JOIN User as tu ON tu.id = tw.user.id
			WHERE t.sourceWalletId = :walletId OR t.targetWalletId = :walletId
			ORDER BY t.createdAt DESC
			""")
	Page<TransactionHistoryView> findWalletHistory(@Param("walletId") Long walletId, Pageable pageable);

	@Query("""
			SELECT  t.id AS id,
			        t.sourceWalletId AS sourceWalletId,
			        t.targetWalletId AS targetWalletId,
			        t.amount AS amount,
			        t.type AS type,
			        t.status AS status,
			        t.createdAt AS createdAt
			FROM Transaction t
			WHERE t.status = com.ahmedesawy.E_Wallet.enums.TransactionStatus.FAILED
			    OR t.amount >= :highValueThreshold
			ORDER BY t.createdAt DESC
			""")
	Page<SuspiciousTransactionView> findSupiciousTransaction(
			@Param("highValueThreshold") BigDecimal highValueThreshold,
			Pageable pageable);

}
