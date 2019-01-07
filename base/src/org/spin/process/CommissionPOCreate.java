/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * or (at your option) any later version.										*
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net or http://www.adempiere.net/license.html         *
 *****************************************************************************/

package org.spin.process;

import java.util.Hashtable;

import org.compiere.model.MBPartner;
import org.compiere.model.MCommission;
import org.compiere.model.MCommissionAmt;
import org.compiere.model.MCommissionRun;
import org.compiere.model.MDocType;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.util.Env;

/** Generated Process for (Create Purchase Order)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.1
 */
public class CommissionPOCreate extends CommissionPOCreateAbstract {
	/**	Invoices created	*/
	private int created = 0;
	/**	Message	*/
	private StringBuffer generatedDocuments = new StringBuffer();
	/**	Invoices	*/
	private Hashtable<Integer, MOrder> orders;
	
	/**
	 *  Perform process.
	 *  @return Message (variables are parsed)
	 *  @throws Exception if not successful
	 */
	protected String doIt() throws Exception {
		log.info("doIt - C_CommissionRun_ID=" + getRecord_ID());
		//	Load Data
		MCommissionRun commissionRun = new MCommissionRun (getCtx(), getRecord_ID(), get_TrxName());
		if (commissionRun.getC_CommissionRun_ID() == 0)
			throw new IllegalArgumentException("@C_CommissionRun_ID@ @NotFound@");
		if (Env.ZERO.compareTo(commissionRun.getGrandTotal()) == 0)
			throw new IllegalArgumentException("@GrandTotal@ = 0");
		//	For Completed Commission
		if(!commissionRun.getDocStatus().equals(MCommissionRun.ACTION_Complete)) {
			throw new IllegalArgumentException("@C_CommissionRun_ID@ @NotValid@");
		}
		//	
		MCommission commissionDefinition = new MCommission (getCtx(), commissionRun.getC_Commission_ID(), get_TrxName());
		if (commissionDefinition.getC_Commission_ID() == 0)
			throw new IllegalArgumentException("@C_Commission_ID@ @NotFound@");
		if (commissionDefinition.getC_Charge_ID() == 0)
			throw new IllegalArgumentException("@C_Commission_ID@ - (@C_Charge_ID@) @NotFound@");
		//	
		orders = new Hashtable<>();
		//	Get lines
		commissionRun.getCommissionAmtList().stream()
			.filter(commissionAmt -> commissionAmt.getCommissionAmt() != null 
				&& commissionAmt.getCommissionAmt().compareTo(Env.ZERO) > 0).forEach(commissionAmt -> {
			MOrder order = getOrder(commissionDefinition, commissionRun, commissionAmt);
			createOrderLine(commissionAmt, order, commissionDefinition.getC_Charge_ID());
		});
		//	Process orders
		if(orders.size() > 0) {
			orders.entrySet().stream().forEach(orderSet ->{
				MOrder order = orderSet.getValue();
				order.processIt(getDocAction());
			});
		}
		//
		return "@Created@ " + created + (generatedDocuments.length() > 0? " [" + generatedDocuments + "]": "");
	}	//	doIt
	
	/**
	 * Create Invoice header
	 */
	private MOrder getOrder(MCommission commissionDefinition, MCommissionRun commissionRun, MCommissionAmt commissionAmt) {
		MOrder order = orders.get(commissionAmt.getC_BPartner_ID());
		if(order != null) {
			return order;
		}
		//	Create Invoice
		order = new MOrder (getCtx(), 0, null);
		order.setClientOrg(commissionAmt.getAD_Client_ID(), commissionAmt.getAD_Org_ID());
		order.setIsSOTrx(false);
		if(getDocTypeId() > 0) {
			order.setC_DocTypeTarget_ID(getDocTypeId());
		} else {
			order.setC_DocTypeTarget_ID();	//	POO
		}
		MBPartner businessPartner = MBPartner.get(getCtx(), commissionAmt.getC_BPartner_ID());
		order.setBPartner(businessPartner);
		order.setSalesRep_ID(getAD_User_ID());	//	caller
		order.setDateOrdered(getDateOrdered());
		order.setDocStatus(MOrder.DOCSTATUS_Drafted);
		//
		if (commissionDefinition.getC_Currency_ID() != order.getC_Currency_ID()) {
			throw new IllegalArgumentException("@CommissionAPInvoiceCurrency@");	//	TODO Translate it: CommissionAPInvoice - Currency of PO Price List not Commission Currency
		}
		//		
		order.saveEx();
		orders.put(commissionAmt.getC_BPartner_ID(), order);
		//	Add to message
		created++;
		addToMessage(order.getDocumentNo());
		return order;
	}
	
	/**
	 * Create line from invoice
	 * @param commissionAmt
	 * @param invoice
	 * @param chargeId
	 */
	private void createOrderLine(MCommissionAmt commissionAmt, MOrder invoice, int chargeId) {
		//	Create Invoice Line
 		MOrderLine orderLine = new MOrderLine(invoice);
		orderLine.setC_Charge_ID(chargeId);
		orderLine.setQty(Env.ONE);
		orderLine.setPrice(commissionAmt.getCommissionAmt());
		orderLine.setTax();
		orderLine.saveEx();
	}


	/**
	 * Add Document Info for message to return
	 * @param documentInfo
	 */
	private void addToMessage(String documentInfo) {
		if(generatedDocuments.length() > 0) {
			generatedDocuments.append(", ");
		}
		//	
		generatedDocuments.append(documentInfo);
	}
}