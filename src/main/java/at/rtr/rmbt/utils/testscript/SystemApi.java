/*******************************************************************************
 * Copyright 2013-2016 alladin-IT GmbH
 * Copyright 2013-2016 Rundfunk und Telekom Regulierungs-GmbH (RTR-GmbH)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package at.rtr.rmbt.utils.testscript;

import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.Random;

public class SystemApi {

	final static DecimalFormat DEFAULT_DECIMAL_FORMAT = new DecimalFormat("##0.00");
	
	static {
		DEFAULT_DECIMAL_FORMAT.setMaximumFractionDigits(2);
	}
	
	public int getCount(Object array) {
		if (array != null && array.getClass().isArray()) {
			return Array.getLength(array);
		}

		return 0;
	}

	public boolean isEmpty(Object array) {
		return getCount(array) == 0;
	}
	
	public boolean isNull(Object o) {
		return o == null;
	}
	
	public static String getRandomUrl(String prefix, String suffix, int length) {
        char[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f' };
        
        StringBuilder randomUrl = new StringBuilder();
        randomUrl.append(prefix);
        Random rnd = new Random();
        
        for (int i = 0; i < length; i++) {
        	randomUrl.append(digits[rnd.nextInt(16)]);
        }
        
        randomUrl.append(suffix);
        return randomUrl.toString();
	}
	
	public Object debug(Object toLog) {
		System.out.println("QoSLOG: " + toLog);
		return toLog;
	}
}
