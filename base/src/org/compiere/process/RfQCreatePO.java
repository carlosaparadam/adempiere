/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.compiere.process;

import java.math.BigDecimal;

import org.compiere.model.I_C_Campaign;
import org.compiere.model.I_C_Invoice;
import org.compiere.model.I_C_Project;
import org.compiere.model.I_C_ProjectPhase;
import org.compiere.model.I_C_ProjectTask;
import org.compiere.model.I_C_RfQ;
import org.compiere.model.MBPartner;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MRfQ;
import org.compiere.model.MRfQLine;
import org.compiere.model.MRfQResponse;
import org.compiere.model.MRfQResponseLine;
import org.compiere.model.MRfQResponseLineQty;

/**
 * 	Create RfQ PO.
 *	Create purchase order(s) for the resonse(s) and lines marked as 
 *	Selected Winner using the selected Purchase Quantity (in RfQ Line Quantity) 
 *	
 *  @author Jorg Janke
 *  @version $Id: RfQCreatePO.java,v 1.2 2006/07/30 00:51:02 jjanke Exp $
 *  
 *  @author Teo Sarca, teo.sarca@gmail.com
 *  	<li>BF [ 2892588 ] Create PO from RfQ is not setting correct the price fields
 *  		https://sourceforge.net/tracker/?func=detail&aid=2892588&group_id=176962&atid=879332
 */
public class RfQCreatePO extends RfQCreatePOAbstract {
	/**
	 * 	Prepare
	 */
	protected void prepare () {
		super.prepare();
	}	//	prepare

	/**
	 * 	Process.
	 * 	Create purchase order(s) for the resonse(s) and lines marked as 
	 * 	Selected Winner using the selected Purchase Quantity (in RfQ Line Quantity) . 
	 * 	If a Response is marked as Selected Winner, all lines are created 
	 * 	(and Selected Winner of other responses ignored).  
	 * 	If there is no response marked as Selected Winner, the lines are used.
	 *	@return message
	 */
	protected String doIt () throws Exception
	{
		MRfQ rfq = new MRfQ (getCtx(), getRecord_ID(), get_TrxName());
		if (rfq.get_ID() == 0)
			throw new IllegalArgumentException("No RfQ found");
		log.info(rfq.toString());
		
		//	Complete 
		MRfQResponse[] responses = rfq.getResponses(true, true);
		log.config("#Responses=" + responses.length);
		if (responses.length == 0)
			throw new IllegalArgumentException("No completed RfQ Responses found");
		
		//	Winner for entire RfQ
		for (MRfQResponse response : responses) {
			if (!response.isSelectedWinner())
				continue;
			//
			MBPartner bp = new MBPartner(getCtx(), response.getC_BPartner_ID(), get_TrxName());
			log.config("Winner=" + bp);
			MOrder order = new MOrder (getCtx(), 0, get_TrxName());
			order.setIsSOTrx(false);
			if (getDocTypeId() != 0) {
				order.setC_DocTypeTarget_ID(getDocTypeId());
			} else {
				order.setC_DocTypeTarget_ID();
			}
			order.setBPartner(bp);
			order.setC_BPartner_Location_ID(response.getC_BPartner_Location_ID());
			order.setSalesRep_ID(rfq.getSalesRep_ID());
			//	Set default values
			order.set_ValueOfColumn(I_C_RfQ.COLUMNNAME_C_RfQ_ID, rfq.getC_RfQ_ID());
			//	
			int campaignId = rfq.get_ValueAsInt(I_C_Campaign.COLUMNNAME_C_Campaign_ID);
			int user1Id = rfq.get_ValueAsInt(I_C_Invoice.COLUMNNAME_User1_ID);
			int projectId = rfq.get_ValueAsInt(I_C_Project.COLUMNNAME_C_Project_ID);
			if(campaignId > 0){
				order.set_ValueOfColumn(I_C_Campaign.COLUMNNAME_C_Campaign_ID, campaignId);
			}
			if(user1Id > 0){
				order.set_ValueOfColumn(I_C_Invoice.COLUMNNAME_User1_ID, user1Id);
			}
			if(projectId > 0){
				order.set_ValueOfColumn(I_C_Project.COLUMNNAME_C_Project_ID, projectId);
			}
			if (response.getDateWorkComplete() != null)
				order.setDatePromised(response.getDateWorkComplete());
			else if (rfq.getDateWorkComplete() != null)
				order.setDatePromised(rfq.getDateWorkComplete());
			order.saveEx();
			//
			for (MRfQResponseLine line : response.getLines(false)) {
				if (!line.isActive())
					continue;
				//	Response Line Qty
				for (MRfQResponseLineQty lineQty : line.getQtys(false)) {
					//	Create PO Lline for all Purchase Line Qtys
					if (lineQty.getRfQLineQty().isActive() && lineQty.getRfQLineQty().isPurchaseQty()) {
						MOrderLine orderLine = new MOrderLine (order);
						orderLine.setM_Product_ID(line.getRfQLine().getM_Product_ID(), 
							lineQty.getRfQLineQty().getC_UOM_ID());
						orderLine.setDescription(line.getDescription());
						orderLine.setQty(lineQty.getRfQLineQty().getQty());
						BigDecimal price = lineQty.getNetAmt();
						orderLine.setPrice();
						orderLine.setPrice(price);
						MRfQLine rFqline = line.getRfQLine();
						int projectTaskId = rFqline.get_ValueAsInt(I_C_ProjectTask.COLUMNNAME_C_ProjectTask_ID);
						int projectPhaseId = rFqline.get_ValueAsInt(I_C_ProjectTask.COLUMNNAME_C_ProjectPhase_ID);
						//	Validate
						if(projectPhaseId > 0) {
							orderLine.set_ValueOfColumn(I_C_ProjectPhase.COLUMNNAME_C_ProjectPhase_ID, projectPhaseId);
						}
						if(projectTaskId > 0) {
							orderLine.set_ValueOfColumn(I_C_ProjectTask.COLUMNNAME_C_ProjectTask_ID, projectTaskId);
						}
						orderLine.saveEx();
					}
				}
			}
			response.setC_Order_ID(order.getC_Order_ID());
			response.saveEx();
			return order.getDocumentNo();
		}

		
		//	Selected Winner on Line Level
		int noOrders = 0;
		for (int i = 0; i < responses.length; i++)
		{
			MRfQResponse response = responses[i];
			MBPartner bp = null;
			MOrder order = null;
			//	For all Response Lines
			MRfQResponseLine[] lines = response.getLines(false);
			for (int j = 0; j < lines.length; j++)
			{
				MRfQResponseLine line = lines[j];
				if (!line.isActive() || !line.isSelectedWinner())
					continue;
				//	New/different BP
				if (bp == null || bp.getC_BPartner_ID() != response.getC_BPartner_ID())
				{
					bp = new MBPartner(getCtx(), response.getC_BPartner_ID(), get_TrxName());
					order = null;
				}
				log.config("Line=" + line + ", Winner=" + bp);
				//	New Order
				if (order == null)
				{
					order = new MOrder (getCtx(), 0, get_TrxName());
					order.setIsSOTrx(false);
					order.setC_DocTypeTarget_ID();
					order.setBPartner(bp);
					order.setC_BPartner_Location_ID(response.getC_BPartner_Location_ID());
					order.setSalesRep_ID(rfq.getSalesRep_ID());
					order.saveEx();
					noOrders++;
					addLog(0, null, null, order.getDocumentNo());
				}
				//	For all Qtys
				MRfQResponseLineQty[] qtys = line.getQtys(false);
				for (int k = 0; k < qtys.length; k++)
				{
					MRfQResponseLineQty qty = qtys[k];
					if (qty.getRfQLineQty().isActive() && qty.getRfQLineQty().isPurchaseQty())
					{
						MOrderLine ol = new MOrderLine (order);
						ol.setM_Product_ID(line.getRfQLine().getM_Product_ID(), 
							qty.getRfQLineQty().getC_UOM_ID());
						ol.setDescription(line.getDescription());
						ol.setQty(qty.getRfQLineQty().getQty());
						BigDecimal price = qty.getNetAmt();
						ol.setPrice();
						ol.setPrice(price);
						ol.saveEx();
					}
				}	//	for all Qtys
			}	//	for all Response Lines
			if (order != null)
			{
				response.setC_Order_ID(order.getC_Order_ID());
				response.saveEx();
			}
		}
		
		return "#" + noOrders;
	}	//	doIt
}	//	RfQCreatePO
