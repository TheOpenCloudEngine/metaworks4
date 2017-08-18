package org.metaworks.common.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by uengine on 2015. 5. 22..
 */
public class DateTest {

    public static void main(String[] args) throws Exception {

        //리턴할 billingDate
        Date billingDate = null;

        //account 의 bdc
        int bdc = 15;

        //actualBcd 는 해당 달의 마지막 day 를 수렴해 재조정한 bcd 이다.
        int actualBcd = bdc;

        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd");
        Date effectiveDate = sd.parse("2017-02-27");

        Calendar cal = Calendar.getInstance();
        cal.setTime(effectiveDate);

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        //effectiveDate 달의 마지막 day 를 구한다.
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        int actualLastBcdDay = cal.get(Calendar.DAY_OF_MONTH);

        //actualBcd 가 actualLastBcdDay 보다 클 경우 actualBcd 를 재조정한다.
        if (actualBcd > actualLastBcdDay) {
            actualBcd = actualLastBcdDay;
        }

        //actualBcd 가 effective_date dd 보다 같거나 클 경우, billing_date 해당 달의 actualBcd 이다.
        if (actualBcd >= day) {
            cal.set(Calendar.DAY_OF_MONTH, actualBcd);
            System.out.println(sd.format(cal.getTime()));
        }
        //actualBcd 가 effective_date dd 보다 작을 경우, billing_date 는 해당 달++ 달의 ACCOUNT bcd 대입 날짜이다.
        else {
            //다음달의 마지막 day 를 구한다.
            cal.set(Calendar.MONTH, month + 1);
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            actualLastBcdDay = cal.get(Calendar.DAY_OF_MONTH);

            //다음달의 actualBcd 를 구한다.
            actualBcd = bdc;
            if (actualBcd > actualLastBcdDay) {
                actualBcd = actualLastBcdDay;
            }
            //다음달의 최종 bcd 결제일을 구한다.
            cal.set(Calendar.DAY_OF_MONTH, actualBcd);

            System.out.println(sd.format(cal.getTime()));
        }

        System.out.println(year);
        System.out.println(month + 1);
        System.out.println(day);

        System.out.println("actualBcd : " + actualBcd);
    }
}
