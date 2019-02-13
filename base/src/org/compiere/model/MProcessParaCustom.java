/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * This program is free software; you can redistribute it and/or modify it    		 *
 * under the terms version 2 or later of the GNU General Public License as published *
 * by the Free Software Foundation. This program is distributed in the hope   		 *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied 		 *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           		 *
 * See the GNU General Public License for more details.                       		 *
 * You should have received a copy of the GNU General Public License along    		 *
 * with this program; if not, write to the Free Software Foundation, Inc.,    		 *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     		 *
 * For the text or an alternative of this public license, you may reach us    		 *
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Yamel Senih www.erpya.com				  		                 *
 *************************************************************************************/
package org.compiere.model;

import java.sql.ResultSet;
import java.util.Properties;

/**
 * Customization handler
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 */
public class MProcessParaCustom extends X_AD_ProcessParaCustom {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7788247772367181508L;
	
	public MProcessParaCustom(Properties ctx, int AD_ProcessParaCustom_ID, String trxName) {
		super(ctx, AD_ProcessParaCustom_ID, trxName);
	}

	public MProcessParaCustom(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
	
	/**
	 * Create from parent
	 * @param customProcess
	 */
	public MProcessParaCustom(MProcessCustom customProcess) {
		this(customProcess.getCtx(), 0, customProcess.get_TrxName());
		setAD_ProcessCustom_ID(customProcess.getAD_ProcessCustom_ID());
	}
	
	@Override
	public void setAD_Process_Para_ID(int processParaId) {
		super.setAD_Process_Para_ID(processParaId);
		if(processParaId > 0) {
			MProcessPara processParameter = MProcessPara.get(getCtx(), processParaId);
			setIsActive(processParameter.isActive());
			setSeqNo(processParameter.getSeqNo());
		}
	}
}
