package com.cognizant.cognizantits.engine.custom.methods;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cognizant.cognizantits.engine.commands.General;
import com.cognizant.cognizantits.engine.core.CommandControl;
import com.cognizant.cognizantits.engine.core.Control;
import com.cognizant.cognizantits.engine.support.Status;
import com.cognizant.cognizantits.engine.support.methodInf.Action;
import com.cognizant.cognizantits.engine.support.methodInf.InputType;
import com.cognizant.cognizantits.engine.support.methodInf.ObjectType;

public class Utilities extends General {

	public Utilities(CommandControl cc) {
		super(cc);
	}

	@Action(object = ObjectType.WEBSERVICE, desc = "Stores a UUID in a datasheet", input = InputType.YES)
	public void StoreUUID() {
		try {
			UUID uuid = UUID.randomUUID();
			String UUIDString = uuid.toString();
			String strObj = Input;
			if (strObj.matches(".*:.*")) {
				String sheetName = strObj.split(":", 2)[0];
				String columnName = strObj.split(":", 2)[1];
				userData.putData(sheetName, columnName, UUIDString);
				Report.updateTestLog(Action, "UUID [" + UUIDString + "] is stored in " + strObj, Status.DONE);
			}
		} catch (Exception ex) {
			Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
			Report.updateTestLog(Action, "Something went wrong in storing the UUID" + "\n" + ex.getMessage(),
					Status.FAILNS);

		}
	}
}
