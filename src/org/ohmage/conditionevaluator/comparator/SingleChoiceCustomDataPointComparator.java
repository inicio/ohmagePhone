/*******************************************************************************
 * Copyright 2011 The Regents of the University of California
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
package org.ohmage.conditionevaluator.comparator;

import org.ohmage.conditionevaluator.DataPoint;

/**
 * Comparator for the single_choice_custom prompt type.
 * 
 * @author Mohamad Monibi
 *
 */
public class SingleChoiceCustomDataPointComparator extends AbstractDataPointComparator {
	
	// The methods here all need to be updated to support conditions after changes made due to 2.9 api changes
	// dataPoint.getValue() will now return a String instead of an Integer
	// the integer key for that string needs to be looked up in the campaign config
	// then that key can be compared to the value parameter (after casting to Integer)

	@Override
	protected boolean equals(DataPoint dataPoint, String value) {
//		Integer dataPointValue = (Integer) dataPoint.getValue();
//        Integer valueToCompare = Integer.parseInt(value);
//        
//        if (dataPointValue.compareTo(valueToCompare) == 0) {
//            return true;
//        } else {
//        	return false;
//        }
		return false;
	}

	@Override
	protected boolean greaterThan(DataPoint dataPoint, String value) {
//		Integer dataPointValue = (Integer) dataPoint.getValue();
//		Integer valueToCompare = Integer.parseInt(value);
//        
//        if (dataPointValue.compareTo(valueToCompare) > 0) {
//            return true;
//        } else {
//        	return false;
//        }
		return false;
	}

	@Override
	protected boolean greaterThanOrEquals(DataPoint dataPoint, String value) {
//		Integer dataPointValue = (Integer) dataPoint.getValue();
//		Integer valueToCompare = Integer.parseInt(value);
//        
//        if (dataPointValue.compareTo(valueToCompare) >= 0) {
//            return true;
//        } else {
//        	return false;
//        }
		return false;
	}

	@Override
	protected boolean lessThan(DataPoint dataPoint, String value) {
//		Integer dataPointValue = (Integer) dataPoint.getValue();
//		Integer valueToCompare = Integer.parseInt(value);
//        
//        if (dataPointValue.compareTo(valueToCompare) < 0) {
//            return true;
//        } else {
//        	return false;
//        }
		return false;
	}

	@Override
	protected boolean lessThanOrEquals(DataPoint dataPoint, String value) {
//		Integer dataPointValue = (Integer) dataPoint.getValue();
//		Integer valueToCompare = Integer.parseInt(value);
//        
//        if (dataPointValue.compareTo(valueToCompare) <= 0) {
//            return true;
//        } else {
//        	return false;
//        }
		return false;
	}

	@Override
	protected boolean notEquals(DataPoint dataPoint, String value) {
//		Integer dataPointValue = (Integer) dataPoint.getValue();
//		Integer valueToCompare = Integer.parseInt(value);
//        
//        if (dataPointValue.compareTo(valueToCompare) != 0) {
//            return true;
//        } else {
//        	return false;
//        }
		return false;
	}

}
