class EGN {
	/*
	 * Побитово кодиране: 0-13 - последни 4 цифри 14-18 - ден 19-22 - месец 23-29 -
	 * година (гг) 30 - 0:19..., 1:20... 31 - 0: вярно ЕГН, 1 - невярно
	 */
	private int val;

	// ИНСТРУМЕНТИ
	// Метод за връщане като тип int на съдържимото в len бита,
	// започвайки от start-овия.
	private int getBitsVal(int start, int len) {
		int mask = ((1 << len) - 1) << (start + 1 - len);// маска за изчистване на ненужните битове
		return (val & mask) >>> (start + 1 - len);// изчистване и преместване на младше място
	}

	public boolean leapYear(int y)// Дали година y е високосна?
	{
		if (y % 400 == 0)
			return true;// Ако се дели на 400 - да
		if (y % 100 == 0)
			return false;// иначе, ако се дели на 100 - не
		return ((y & 0b11) == 0);// иначе - доколкото се дели на 4
	}

	public boolean correctDate(int y, int m, int d)// дали дата е коректна?
	{
		if (m < 1 || m > 12)
			return false;// месец: от 1 до 12
		if (d < 1 || d > 31)
			return false;// ден: от 1 до 31
		// месеци, които нямат 31 дена:
		if ((m == 2 || m == 4 || m == 6 || m == 9 || m == 11) && d == 31)
			return false;
		// няма 30.02.
		if (m == 2 && d == 30)
			return false;
		// няма 29.02 през невисокосните години
		if (m == 2 && d == 29 && !leapYear(y))
			return false;
		return true;
	}

	// КОНСТРУКТОРИ
	public EGN() {// Празният конструктор прави "празно" некоректно ЕГН
		val = 0x80000000;
	}

	public EGN(int d)// Конструктор по вече кодирано ЕГН
	{
		val = d;
	}

	// Конструктор по низ
	public EGN(String s) {
		// 1. 10 цифрови символа
		// 2. отделяме ден, месец, година.
		// Ако мес. е 1-12 - 19..., ако е от 41-52 - 20...
		// 3. Проверка за коректност на датата
		// 4. Проверка на чек сумата
		val = 0x80000000;// текущо е "празното" (и некоректно) ЕГН
		if (s.length() != 10)
			return;// Ако низът не е точно от 10 символа - остава некоректно
		// Ако някой от символите не е цифра - остава некоректно
		for (int i = 0; i < 10; i++)
			if (s.charAt(i) < '0' || s.charAt(i) > '9')
				return;
		int y, m, d, last4 = 0;
		y = 10 * (s.charAt(0) - '0') + s.charAt(1) - '0';// последните две цифри на годината като число
		m = 10 * (s.charAt(2) - '0') + s.charAt(3) - '0';// месецът като число (или +40)
		if (m > 40) {
			m -= 40;
			y += 2000;
		} else
			y += 1900;// месец и година на раждане в разкодиран вид
		d = 10 * (s.charAt(4) - '0') + s.charAt(5) - '0';// ден на раждане като число
		for (int i = 6; i < 10; i++)
			last4 = 10 * last4 + s.charAt(i) - '0';// последните 4 цифри като число
		setVal(y, m, d, last4);// Кодира ЕГН по дата на раждане и последни 4 цифри
	}

	// ГЕТЪРИ (достъп - четене до кодираните свойства)
	public int getLast4() // Последните 4 цифри (14 бита:от 13-ти до нулев)
	{
		return getBitsVal(13, 14);
	}

	public boolean isValid() {
		return val > 0;// Ако старшият бит е 1 (некоректно ЕГН),
						// val се смята за отрицателно число в допълнителен код
	}

	public int getDay() // Ден на раждане
	{
		return getBitsVal(18, 5);
	}

	public int getMonth() // Месец на раждане
	{
		return getBitsVal(22, 4);
	}

	public int getCentury() // Век (0-20-ти, 1-21-ви)
	{
		return getBitsVal(30, 1);
	}

	public int getYear()// година на раждане
	{
		if (getCentury() == 1)
			return 2000 + getBitsVal(29, 7);
		return 1900 + getBitsVal(29, 7);
	}

	public int getVal() // Гетър за кодираното ЕГН (като число тип int)
	{
		return val;
	}

	public char getGender() // Пол по четността на предпоследната цифра
	{
		if (!isValid())
			return '?';// не е определен, ако ЕГН-то е невалидно
		int last4 = getLast4();
		last4 /= 10;
		return (last4 & 1) == 1 ? 'F' : 'M';// Нечетна-женски пол, четна-мъжки пол
	}

	// СЕТЪР
	// (Кодиране по данни за дата на раждане и последни 4 цифри)
	private void setVal(int bYear, int bMonth, int bDay, int L4) {
		val = L4;
		val |= (bDay << 14);
		val |= (bMonth << 19);
		if (bYear >= 2000) {
			val |= ((bYear - 2000) << 23);
			val |= 0x40000000;// и 21-ви век
		} else
			val |= ((bYear - 1900) << 23);
		String s = toString();// Превръщане на val в низ от 10 цифри
								// (Тук старшият бит на val е още 0)
		// Проверка на чек-сумата
		int c = 0;
		for (int i = 8; i >= 0; i--)
			c = (c << 1) + s.charAt(i) - '0';
		c <<= 1;
		c = (c % 11) % 10;
		// Ако чек-сумата не съвпада с последната цифре или датата на раждане некоректна
		// -
		// ЕГН-то не е коректно (сетваме най-старшия бит)
		if (c != getLast4() % 10 || !correctDate(bYear, bMonth, bDay))
			val |= 0x80000000;
	}

	// ДРУГИ МЕТОДИ
	public String getBithDate() {
		if (!isValid())
			return "Invalid";
		return String.format("%02d.%02d.%d", getDay(), getMonth(), getYear());
	}

	@Override
	public String toString() // Превръщане в низ
	{
		if (!isValid())
			return "Incorrect!";// некоректно ЕГН
		int m = getCentury() == 1 ? getMonth() + 40 : getMonth();// месец на раждане (+40 след 2000 г.)
		int y = getCentury() == 1 ? getYear() - 2000 : getYear() - 1900;// младши две цифри на годината на раждане
		return String.format("%02d%02d%02d%04d", y, m, getDay(), getLast4());
	}
}

public class Main {
	public static void main(String[] args) {
		EGN egn = new EGN("0847227720");
		System.out.println("Encoded: " + egn.getVal());
		System.out.println(egn);
		System.out.println(egn.getGender());
		System.out.println(egn.getBithDate());
	}
}
