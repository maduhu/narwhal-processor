package net.canadensys.processor.datetime;

import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.Map;

import javax.time.LocalDate;
import javax.time.MonthOfYear;
import javax.time.calendrical.Calendrical;
import javax.time.extended.Year;
import javax.time.extended.YearMonth;
import javax.time.format.CalendricalParseException;
import javax.time.format.DateTimeFormatter;
import javax.time.format.DateTimeFormatters;

import net.canadensys.processor.DataProcessor;
import net.canadensys.processor.ProcessingResult;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Data processor to handle dates including partial dates.
 * TODO : Test non-US locals.
 * @author canadensys
 *
 */
public class DateProcessor implements DataProcessor{
		
	public static final int YEAR_IDX = 0;
	public static final int MONTH_IDX = 1;
	public static final int DAY_IDX = 2;
	
	private String dateName = "date";
	private String yearName = "year";
	private String monthName = "month";
	private String dayName = "day";
	
	//Only USE_NULL make sense here
	private ErrorHandlingModeEnum errorHandlingMode = ErrorHandlingModeEnum.USE_NULL;
	
	private static final DateTimeFormatter MMM_DD_YYYY_PATTERN = DateTimeFormatters.pattern("MMM d[d] yyyy", Locale.US);
	private static final DateTimeFormatter YYYY_MMM_DD_PATTERN = DateTimeFormatters.pattern("yyyy MMM d[d]", Locale.US);
	private static final DateTimeFormatter DD_MMM_YYYY_PATTERN = DateTimeFormatters.pattern("d[d] MMM yyyy", Locale.US);
	
	private static final DateTimeFormatter PARTIAL_DATE_PATTERN = DateTimeFormatters.pattern("yyyy[-MM[-dd]]", Locale.US);
	private static final DateTimeFormatter PARTIAL_MONTH_YEAR_PATTERN = DateTimeFormatters.pattern("MMM yyyy", Locale.US);
	private static final DateTimeFormatter PARTIAL_MONTH_PATTERN = DateTimeFormatters.pattern("MMM", Locale.US);
	
	public DateProcessor(String dateName, String yearName, String monthName, String dayName){
		this.dateName = dateName;
		this.yearName = yearName;
		this.monthName = monthName;
		this.dayName = dayName;
	}
	
	/**
	 * Date and partial date Bean processing function.
	 * @param in Java bean containing the date property as String
	 * @param out Java bean containing the 3 properties that will keep the 3 parts of the date
	 * @param params Will be ignored so use null, could eventually be used to include decades
	 */
	@Override
	public void processBean(Object in, Object out, Map<String, Object> params, ProcessingResult result) {
		try {
			Integer[] output = new Integer[3];
			String textDate = (String)PropertyUtils.getSimpleProperty(in, dateName);
			
			process(textDate,output,result);
						
			PropertyUtils.setSimpleProperty(out, yearName, output[YEAR_IDX]);
			PropertyUtils.setSimpleProperty(out, monthName, output[MONTH_IDX]);
			PropertyUtils.setSimpleProperty(out, dayName, output[DAY_IDX]);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Date processing function
	 * @param dateText a test representing the date or partial-date
	 * @param output initialized array(size==3) that will contain the parsed data(year,month,day) or null.
	 * @param result optional processing result
	 */
	public void process(String dateText, Integer[] output, ProcessingResult result){
		if(StringUtils.isBlank(dateText)){
			return;
		}
		
		try{
			//try ISO 8601 (with partial date like 2008 or 2008-12)
			setPartialDate(output,PARTIAL_DATE_PATTERN.parseBest(dateText, LocalDate.rule(),YearMonth.rule(),Year.rule()));
			return;
		}
		catch(CalendricalParseException cpe){}
		
		try{
			//try format like Jul 6 1987
			setPartialDate(output,MMM_DD_YYYY_PATTERN.parse(dateText, LocalDate.rule()));
			return;
		}
		catch(CalendricalParseException cpe){}
		
		try{
			//try format like 1987 Jul 6
			setPartialDate(output,YYYY_MMM_DD_PATTERN.parse(dateText, LocalDate.rule()));
			return;
		}
		catch(CalendricalParseException cpe){}
		
		try{
			//try format like 6 Jul 1986
			setPartialDate(output,DD_MMM_YYYY_PATTERN.parse(dateText, LocalDate.rule()));
			return;
		}
		catch(CalendricalParseException cpe){}

		//PARTIAL DATE
		try{
			//try format like Jun 1895
			setPartialDate(output,PARTIAL_MONTH_YEAR_PATTERN.parse(dateText, YearMonth.rule()));
			return;
		}
		catch(CalendricalParseException cpe){}
		
		try{
			//try format like Jun
			setPartialDate(output,PARTIAL_MONTH_PATTERN.parse(dateText, MonthOfYear.rule()));
			return;
		}
		catch(CalendricalParseException cpe){}
		
		if(result != null){
			result.addError("The date ["+dateText+"] could not be processed.");
		}
	}
	

	/**
	 * Fill the partialDate array according to the content of the Calendrical object.
	 * @param partialDate initialized array of size 3
	 * @param cal
	 */
	protected void setPartialDate(Integer[] partialDate, Calendrical cal){
		if(cal instanceof LocalDate){
			LocalDate lc = (LocalDate)cal;
			partialDate[DAY_IDX] = lc.getDayOfMonth();
			partialDate[MONTH_IDX] = lc.getMonthOfYear().getValue();
			partialDate[YEAR_IDX] = lc.getYear();			
		}
		else if(cal instanceof YearMonth){
			YearMonth ym = (YearMonth)cal; 
			partialDate[MONTH_IDX] = ym.getMonthOfYear().getValue();
			partialDate[YEAR_IDX] = ym.getYear();
		}
		else if(cal instanceof Year){
			partialDate[YEAR_IDX] = ((Year)cal).getValue();
		}
		else if(cal instanceof MonthOfYear){
			partialDate[MONTH_IDX] = ((MonthOfYear)cal).getValue();
		}
		else{
			throw new UnsupportedOperationException();
		}
	}
	
	@Override
	public ErrorHandlingModeEnum getErrorHandlingMode() {
		return errorHandlingMode;
	}
}
