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
public class MFieldCustom extends X_AD_FieldCustom {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7788247772367181508L;
	
	public MFieldCustom(Properties ctx, int AD_FieldCustom_ID, String trxName) {
		super(ctx, AD_FieldCustom_ID, trxName);
	}

	public MFieldCustom(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**
	 * Create from parent
	 * @param customTab
	 */
	public MFieldCustom(MTabCustom customTab) {
		this(customTab.getCtx(), 0, customTab.get_TrxName());
		setAD_TabCustom_ID(customTab.getAD_TabCustom_ID());
	}
	
	@Override
	public void setAD_Field_ID(int fieldId) {
		super.setAD_Field_ID(fieldId);
		if(fieldId > 0) {
			MField field = new MField(getCtx(), fieldId, get_TrxName());
			setIsActive(field.isActive());
			setIsDisplayed(field.isDisplayed());
			setSeqNo(field.getSeqNo());
			setSeqNoGrid(field.getSeqNoGrid());
		}
	}
}
