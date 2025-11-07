package com.simant.utils;


import com.sacbpp.core.bytes.ByteArrayFactory;
import com.sacbpp.core.utils.Utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Hashtable;
import java.util.Locale;

/**
 * The type Currency table.
 */
public final class CurrencyTable {

    private static Hashtable currencyTable;

    static {
        currencyTable = new Hashtable();

        currencyTable.put(0x784, "AED");
        currencyTable.put(0x971, "AFN");
        currencyTable.put(0x008, "ALL");
        currencyTable.put(0x051, "AMD");
        currencyTable.put(0x532, "ANG");
        currencyTable.put(0x973, "AOA");
        currencyTable.put(0x032, "ARS");
        currencyTable.put(0x036, "AUD");
        currencyTable.put(0x533, "AWG");
        currencyTable.put(0x944, "AZN");
        currencyTable.put(0x977, "BAM");
        currencyTable.put(0x052, "BBD");
        currencyTable.put(0x050, "BDT");
        currencyTable.put(0x975, "BGN");
        currencyTable.put(0x048, "BHD");
        currencyTable.put(0x108, "BIF");
        currencyTable.put(0x060, "BMD");
        currencyTable.put(0x096, "BND");
        currencyTable.put(0x068, "BOB");
        currencyTable.put(0x984, "BOV");
        currencyTable.put(0x986, "BRL");
        currencyTable.put(0x044, "BSD");
        currencyTable.put(0x064, "BTN");
        currencyTable.put(0x072, "BWP");
        currencyTable.put(0x974, "BYR");
        currencyTable.put(0x084, "BZD");
        currencyTable.put(0x124, "CAD");
        currencyTable.put(0x976, "CDF");
        currencyTable.put(0x947, "CHE");
        currencyTable.put(0x756, "CHF");
        currencyTable.put(0x948, "CHW");
        currencyTable.put(0x990, "CLF");
        currencyTable.put(0x152, "CLP");
        currencyTable.put(0x156, "CNY");
        currencyTable.put(0x170, "COP");
        currencyTable.put(0x970, "COU");
        currencyTable.put(0x188, "CRC");
        currencyTable.put(0x931, "CUC");
        currencyTable.put(0x192, "CUP");
        currencyTable.put(0x132, "CVE");
        currencyTable.put(0x203, "CZK");
        currencyTable.put(0x262, "DJF");
        currencyTable.put(0x208, "DKK");
        currencyTable.put(0x214, "DOP");
        currencyTable.put(0x012, "DZD");
        currencyTable.put(0x233, "EEK");
        currencyTable.put(0x818, "EGP");
        currencyTable.put(0x232, "ERN");
        currencyTable.put(0x230, "ETB");
        currencyTable.put(0x978, "\u20ac");
        currencyTable.put(0x242, "FJD");
        currencyTable.put(0x238, "FKP");
        currencyTable.put(0x826, "\u00a3");
        currencyTable.put(0x981, "GEL");
        currencyTable.put(0x936, "GHS");
        currencyTable.put(0x292, "GIP");
        currencyTable.put(0x270, "GMD");
        currencyTable.put(0x324, "GNF");
        currencyTable.put(0x320, "GTQ");
        currencyTable.put(0x328, "GYD");
        currencyTable.put(0x344, "HKD");
        currencyTable.put(0x340, "HNL");
        currencyTable.put(0x191, "HRK");
        currencyTable.put(0x332, "HTG");
        currencyTable.put(0x348, "HUF");
        currencyTable.put(0x360, "IDR");
        currencyTable.put(0x376, "ILS");
        currencyTable.put(0x356, "INR");
        currencyTable.put(0x368, "IQD");
        currencyTable.put(0x364, "IRR");
        currencyTable.put(0x352, "ISK");
        currencyTable.put(0x388, "JMD");
        currencyTable.put(0x400, "JOD");
        currencyTable.put(0x392, "JPY");
        currencyTable.put(0x404, "KES");
        currencyTable.put(0x417, "KGS");
        currencyTable.put(0x116, "KHR");
        currencyTable.put(0x174, "KMF");
        currencyTable.put(0x408, "KPW");
        currencyTable.put(0x410, "KRW");
        currencyTable.put(0x414, "KWD");
        currencyTable.put(0x136, "KYD");
        currencyTable.put(0x398, "KZT");
        currencyTable.put(0x418, "LAK");
        currencyTable.put(0x422, "LBP");
        currencyTable.put(0x144, "LKR");
        currencyTable.put(0x430, "LRD");
        currencyTable.put(0x426, "LSL");
        currencyTable.put(0x440, "LTL");
        currencyTable.put(0x428, "LVL");
        currencyTable.put(0x434, "LYD");
        currencyTable.put(0x504, "MAD");
        currencyTable.put(0x498, "MDL");
        currencyTable.put(0x969, "MGA");
        currencyTable.put(0x807, "MKD");
        currencyTable.put(0x104, "MMK");
        currencyTable.put(0x496, "MNT");
        currencyTable.put(0x446, "MOP");
        currencyTable.put(0x478, "MRO");
        currencyTable.put(0x480, "MUR");
        currencyTable.put(0x462, "MVR");
        currencyTable.put(0x454, "MWK");
        currencyTable.put(0x484, "MXN");
        currencyTable.put(0x979, "MXV");
        currencyTable.put(0x458, "MYR");
        currencyTable.put(0x943, "MZN");
        currencyTable.put(0x516, "NAD");
        currencyTable.put(0x566, "NGN");
        currencyTable.put(0x558, "NIO");
        currencyTable.put(0x578, "NOK");
        currencyTable.put(0x524, "NPR");
        currencyTable.put(0x554, "NZD");
        currencyTable.put(0x512, "OMR");
        currencyTable.put(0x590, "PAB");
        currencyTable.put(0x604, "PEN");
        currencyTable.put(0x598, "PGK");
        currencyTable.put(0x608, "PHP");
        currencyTable.put(0x586, "PKR");
        currencyTable.put(0x985, "PLN");
        currencyTable.put(0x600, "PYG");
        currencyTable.put(0x634, "QAR");
        currencyTable.put(0x946, "RON");
        currencyTable.put(0x941, "RSD");
        currencyTable.put(0x643, "RUB");
        currencyTable.put(0x646, "RWF");
        currencyTable.put(0x682, "SAR");
        currencyTable.put(0x090, "SBD");
        currencyTable.put(0x690, "SCR");
        currencyTable.put(0x938, "SDG");
        currencyTable.put(0x752, "SEK");
        currencyTable.put(0x702, "SGD");
        currencyTable.put(0x654, "SHP");
        currencyTable.put(0x703, "SKK");
        currencyTable.put(0x694, "SLL");
        currencyTable.put(0x706, "SOS");
        currencyTable.put(0x968, "SRD");
        currencyTable.put(0x728, "SSP");
        currencyTable.put(0x678, "STD");
        currencyTable.put(0x760, "SYP");
        currencyTable.put(0x748, "SZL");
        currencyTable.put(0x764, "THB");
        currencyTable.put(0x972, "TJS");
        currencyTable.put(0x795, "TMT");
        currencyTable.put(0x788, "TND");
        currencyTable.put(0x776, "TOP");
        currencyTable.put(0x949, "TRY");
        currencyTable.put(0x780, "TTD");
        currencyTable.put(0x901, "TWD");
        currencyTable.put(0x834, "TZS");
        currencyTable.put(0x980, "UAH");
        currencyTable.put(0x800, "UGX");
        currencyTable.put(0x840, "$");
        currencyTable.put(0x997, "USN");
        currencyTable.put(0x998, "USS");
        currencyTable.put(0x940, "UYI");
        currencyTable.put(0x858, "UYU");
        currencyTable.put(0x860, "UZS");
        currencyTable.put(0x937, "VEF");
        currencyTable.put(0x704, "VND");
        currencyTable.put(0x548, "VUV");
        currencyTable.put(0x882, "WST");
        currencyTable.put(0x950, "XAF");
        currencyTable.put(0x961, "XAG");
        currencyTable.put(0x959, "XAU");
        currencyTable.put(0x955, "XBA");
        currencyTable.put(0x956, "XBB");
        currencyTable.put(0x957, "XBC");
        currencyTable.put(0x958, "XBD");
        currencyTable.put(0x951, "XCD");
        currencyTable.put(0x960, "XDR");
        currencyTable.put(0x952, "XOF");
        currencyTable.put(0x964, "XPD");
        currencyTable.put(0x953, "XPF");
        currencyTable.put(0x962, "XPT");
        currencyTable.put(0x963, "XTS");
        currencyTable.put(0x999, "");
        currencyTable.put(0x886, "YER");
        currencyTable.put(0x710, "ZAR");
        currencyTable.put(0x894, "ZMK");
        currencyTable.put(0x932, "ZWL");

    }

    /**
     * Gets currency.
     *
     * @param code the code
     * @return the currency
     */
    public static String getCurrency(int code) {
        return (String) (currencyTable.get(code));
    }

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        long amount = 499;

        NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.ENGLISH);
        DecimalFormat format = new DecimalFormat("#.00", new DecimalFormatSymbols());
    }

    public static String FormattedAmount(String amount, String currencycode) {
        String txDisplayableAmount = " ";
        if ((amount == null || currencycode == null) || (amount.length()%2 != 0 || currencycode.length()%2 != 0)) {
            return txDisplayableAmount;
        }
        String transactionAmount = Utils.bcdAmountArrayToString(ByteArrayFactory.getInstance().fromHexString(amount));
        int currencyCode = Utils.readShort(ByteArrayFactory.getInstance().fromHexString(currencycode));
        String currencySymbol = CurrencyTable.getCurrency(currencyCode);
        if (currencySymbol == null) {
            txDisplayableAmount = transactionAmount;
        } else {
            txDisplayableAmount = currencySymbol + " " + transactionAmount;
        }
        return txDisplayableAmount;
    }
}