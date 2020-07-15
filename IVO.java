import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDlet;
import javax.microedition.rms.*; // really needed?
import java.io.*;
import javax.microedition.io.*;

public class IVO extends MIDlet implements CommandListener {
  private Display display;

  private Form form = new Form("1V0");

  private Command exit = new Command("EXIT", Command.EXIT, 1);

  private Command submit = new Command("ENTER", Command.SCREEN, 1);
  
  private Command reentry = new Command("INIT", Command.SCREEN, 1);
  
  private Command runcode = new Command("RUN", Command.SCREEN, 1);
  
  private Command setvalue = new Command("SET", Command.SCREEN, 1);
  
  private Command previouscommand;

  private TextField cmdfield = new TextField("CMDA:", "0", 4, TextField.NUMERIC); // 3 OR 4

  private TextField oprfield = new TextField("OPER:", "0", 3, TextField.NUMERIC);
  
  private TextField d1_field = new TextField("ADR1:", "1", 4, TextField.NUMERIC);
  
  private TextField d2_field = new TextField("ADR2:", "1", 4, TextField.NUMERIC);
  
  private TextField rl_field = new TextField("NEW RUNLIMIT:", "32767", 5, TextField.NUMERIC);

  private TextField flofield = new TextField("NUM:", "0", 15, TextField.DECIMAL); // Make the text "00" on fixedpoint!

  private TextField intfield = new TextField("INT:", "0", 11, TextField.NUMERIC);

  private String ephemeral = ""; // I CAN DECLARE HERE ANYTHING AND USE THE STUFF BELOW LIKE GOSUBS
  
  private String rlstring = "";
  
  private String trace1s = "";

  private int entryflag = 0;

  private int DATAMEM = 8001; // 8191 is the addressable space
  private int EXECMEM = 2001; // ANYTHING YOU LIKE, BUT OTHERWISE YOU WAIT TOO LONG FOR THE END IF YOU DID NOT SPECIFY A JUMP BACK

  private short tracer = 0;
  private int instr = 0;
  /* instruction format: */
  /* operator, data address 1 (=result), data address 2 */
  /* every operator is at a command address. */
  private int cmdadr = 0;
  private int opr = 0;
  private int datadr1 = 0;
  private int datadr2 = 0;
  private int pc = 0;
  private int h = 0; /* beware: h & k are used for "transcendental" instructions ... */
  private int i = 0;     /* i & j are auxiliary looping variables */
  private int j = 0;
  private int k = 0; /* ... so ZERO THEM OUT after non-transcendental usage */

  private int xx;
  private int yy;
  private int zz;
  private int[] instruction = new int[EXECMEM];
  private int runlimit = 0;
  /* If this is 0, run unlimited, else break when it becomes 1. */
  /* This is a sort of "safety", to prevent infinite looping. */

  private float f = 0.0f; /* f & g are auxiliary floating point temporary storage */
  private float g = 0.0f;
  private float TOLERANCE = 0.0001f;
  private float PI = 3.14159265358979323846f;
  private float[] datum = new float[DATAMEM];
  
  	private static float sqrt(float num) {
		num = absvalu(num);
		float guess = num / 4;
		int maxrun = 1;
		while(absvalu(guess * guess - num) >= 0.0001f) {
			guess = (num/guess + guess) / 2.0f;
			maxrun++;
			if (maxrun > 200) {
				break;
			}
		}
		return guess;
	}
	
	private static float cbrt(float num) {
		float guess = num / 2;
		int maxrun = 1;
		while(absvalu(guess * guess * guess - num) >= 0.0001f) {
			guess = (num/guess + guess) / 2.0f;
			maxrun++;
			if (maxrun > 200) {
				break;
			}
		}
		return guess;
	}
  
	private static double pwr(double base, int expo) {
		double ff = 1.0;
		int ij;
		if (expo > 1) {
			
			for (ij = 1; ij <= expo; ij++) {
				ff = ff * base;
			}

		} else if (expo < 1) {
			
			expo = -expo;
			
			for (ij = 1; ij <= expo; ij++) {
				ff = ff / base;
			}
			
		} else {
			
			ff = 1.0;

		}

		return ff;

	}

    /* OMITTED - DYSFUNCTIONAL
	public static double lgr(double x) {
		long l = Double.doubleToLongBits(x);
		long expo = ((0x7ff0000000000000L & l) >> 52) - 1023;
		double man = (0x000fffffffffffffL & l) / (double)0x10000000000000L + 1.0;
		double lnm = 0.0;
		double a = (man - 1) / (man + 1);
		for( int n = 1; n < 7; n += 2) {
			lnm += pwr(a, n) / n;
		}
		return 2 * lnm + expo * 0.69314718055994530941723212145818;
	}


	public static float log(float x) {
		return ((float) lgr((double) x));
	}
    */
	
	private static float pow(float base, float expo) {
		return ((float) pwr((double) base, (int) expo));
	}

	private static float exp(float expo) {
		return ((float) pwr(2.71828182845904523536, (int) expo));
	}
	
    private static float absvalu(float x) {
		if (x < 0.0f) {
			return -x;
		} else {
			return x;
		}
	}

  public IVO() {
	
	                                      // Better do stuff BEFORE showing controls.
    for (i=0; i<EXECMEM; i++) {
      instruction[i] = 0;
    }

    for (i=0; i<DATAMEM; i++) {
      datum[i] = 0.0f;
    }
	
	runlimit = 5 * EXECMEM;
	
	display = Display.getDisplay(this);
	form.append("WELCOME TO 1V0: -1show_instructions -2show_data -4enter_data 0NOOP 1JUMP 2IADR 5SADR 6SVAL 7IAAS 8IVAS 9PLUS 10MINS 11MULS 12DIVS 13POXY 14IFRA 15REMN 17AMNT 19PCNT 20SWAP 21FACT 22COPY 23FRIS 24MNMX 25SORT 26CORS 27TURN 28SUMR 29SUSQ 30IXTH 31ABSR 32SQRT 33SQUA 34CBRT 35CUBE 37EXPR 38RADE 39DERA 40SIND 41COSD 42TAND 46MSTD 47ZERO 49RUND 50CEIL 53PLUR 54MINR 55MULR 56DIVR 57PLUN 58MINN 59MULN 60DIVN");
    form.addCommand(exit);
    form.addCommand(reentry);
    form.setCommandListener(this);
  }

  public void startApp() {
    display.setCurrent(form);
  }

  public void pauseApp() {
  }

  public void destroyApp(boolean unconditional) {
  }

  public void commandAction(Command command, Displayable displayable) {
	  
    if (command == submit) {
	  previouscommand = command;
	  // TESTME2 IS MUTABLE HERE AND "REMEMBERS" BEING CHANGED!
	  // ephemeral = cmdfield.getString() + oprfield.getString() + d1_field.getString() + d2_field.getString();
	  ephemeral = cmdfield.getString();                        // Turn the field strings into numbers.
	  cmdadr = java.lang.Integer.parseInt(ephemeral);
	  ephemeral = oprfield.getString();
	  opr = java.lang.Integer.parseInt(ephemeral);
	  ephemeral = d1_field.getString();
	  datadr1 = java.lang.Integer.parseInt(ephemeral);
	  ephemeral = d2_field.getString();
	  datadr2 = java.lang.Integer.parseInt(ephemeral);

      form.removeCommand(submit);        // cycle the available commands
	  form.deleteAll();                  // CLS at first safe place

	  if (cmdadr > 0) {
		  
	    form.deleteAll();                  // CLS at first safe place
	  
	    // THIS WORKS:
	    ephemeral = Integer.toString(cmdadr) + "   " + Integer.toString(opr) + "   " + Integer.toString(datadr1) + "   " + Integer.toString(datadr2);

        form.append(ephemeral);

        if ((datadr1 >= DATAMEM) || (datadr2 >= DATAMEM) ||
           (datadr1 < 0) || (datadr1 < 0) || (cmdadr >= EXECMEM)) {
		  form.append(" CORRECTING RANGE. DATA 0  ... " + (DATAMEM - 1) + ", COMMANDS 0 ... " + (EXECMEM - 1));
        }
		
        /* only positive data addresses - and 0 - are allowed: */
        datadr1 = (int) absvalu(datadr1);
        datadr2 = (int) absvalu(datadr2);

        /* force everything to be within range: */
        if (cmdadr >= EXECMEM) {
          cmdadr = EXECMEM - 1;
        }
        if (datadr1 >= DATAMEM) {
          datadr1 = DATAMEM - 1;
        }
        if (datadr2 >= DATAMEM) {
          datadr2 = DATAMEM - 1;
        }
		
		/* Having read the instruction, compose it and save it. */
  
        xx = opr & 63;
        yy = datadr1 & 8191;
        zz = datadr2 & 8191;
        instr = (xx << 26) | (yy << 13) | zz;
        instruction[cmdadr] = instr;

        ephemeral = " ";
            
        ephemeral = ephemeral + cmdadr + ":" + opr;
            
        ephemeral = ephemeral + "=";
        if (opr == 0) {
          ephemeral = ephemeral + "NOOP";
        } else if (opr == 1) {
          ephemeral = ephemeral + "JUMP";
        } else if (opr == 2) {
          ephemeral = ephemeral + "IADR";
        } else if (opr == 3) {
          ephemeral = ephemeral + "OUTP";
        } else if (opr == 4) {
          ephemeral = ephemeral + "INPT";
        } else if (opr == 5) {
          ephemeral = ephemeral + "SADR";
        } else if (opr == 6) {
          ephemeral = ephemeral + "SVAL";
        } else if (opr == 7) {
          ephemeral = ephemeral + "IAAS";
        } else if (opr == 8) {
          ephemeral = ephemeral + "IVAS";
        } else if (opr == 9) {
          ephemeral = ephemeral + "PLUS";
        } else if (opr == 10) {
          ephemeral = ephemeral + "MINS";
        } else if (opr == 11) {
          ephemeral = ephemeral + "MULS";
        } else if (opr == 12) {
          ephemeral = ephemeral + "DIVS";
        } else if (opr == 13) {
          ephemeral = ephemeral + "POXY";
        } else if (opr == 14) {
          ephemeral = ephemeral + "LOXY";
        } else if (opr == 15) {
          ephemeral = ephemeral + "IFRA";
        } else if (opr == 16) {
          ephemeral = ephemeral + "REMN";
        } else if (opr == 17) {
          ephemeral = ephemeral + "AMNT";
        } else if (opr == 18) {
          ephemeral = ephemeral + "PERD";
        } else if (opr == 19) {
          ephemeral = ephemeral + "PCNT";
        } else if (opr == 20) {
          ephemeral = ephemeral + "SWAP";
        } else if (opr == 21) {
          ephemeral = ephemeral + "FACT";
        } else if (opr == 22) {
          ephemeral = ephemeral + "COPY";
        } else if (opr == 23) {
          ephemeral = ephemeral + "FRIS";
        } else if (opr == 24) {
          ephemeral = ephemeral + "MNMX";
        } else if (opr == 25) {
          ephemeral = ephemeral + "SORT";
        } else if (opr == 26) {
          ephemeral = ephemeral + "CORS";
        } else if (opr == 27) {
          ephemeral = ephemeral + "TURN";
        } else if (opr == 28) {
          ephemeral = ephemeral + "SUMR";
        } else if (opr == 29) {
          ephemeral = ephemeral + "SUSQ";
        } else if (opr == 30) {
          ephemeral = ephemeral + "IXTH";
        } else if (opr == 31) {
          ephemeral = ephemeral + "ABSR";
        } else if (opr == 32) {
          ephemeral = ephemeral + "SQRT";
        } else if (opr == 33) {
          ephemeral = ephemeral + "SQUA";
        } else if (opr == 34) {
          ephemeral = ephemeral + "CBRT";
        } else if (opr == 35) {
          ephemeral = ephemeral + "CUBE";
        } else if (opr == 36) {
          ephemeral = ephemeral + "LNRN";
        } else if (opr == 37) {
          ephemeral = ephemeral + "EXPR";
        } else if (opr == 38) {
          ephemeral = ephemeral + "RADE";
        } else if (opr == 39) {
          ephemeral = ephemeral + "DERA";
        } else if (opr == 40) {
          ephemeral = ephemeral + "SIND";
        } else if (opr == 41) {
          ephemeral = ephemeral + "COSD";
        } else if (opr == 42) {
          ephemeral = ephemeral + "TAND";
        } else if (opr == 43) {
          ephemeral = ephemeral + "ASND";
        } else if (opr == 44) {
          ephemeral = ephemeral + "ACSD";
        } else if (opr == 45) {
          ephemeral = ephemeral + "ATND";
        } else if (opr == 46) {
          ephemeral = ephemeral + "MSTD";
        } else if (opr == 47) {
          ephemeral = ephemeral + "ZERO";
        } else if (opr == 48) {
          ephemeral = ephemeral + "RAND";
        } else if (opr == 49) {
          ephemeral = ephemeral + "RUND";
        } else if (opr == 50) {
          ephemeral = ephemeral + "CEIL";
        } else if (opr == 51) {
          ephemeral = ephemeral + "TANH";
        } else if (opr == 52) {
          ephemeral = ephemeral + "DTNH";
        } else if (opr == 53) {
          ephemeral = ephemeral + "PLUR";
        } else if (opr == 54) {
          ephemeral = ephemeral + "MINR";
        } else if (opr == 55) {
          ephemeral = ephemeral + "MULR";
        } else if (opr == 56) {
          ephemeral = ephemeral + "DIVR";
        } else if (opr == 57) {
          ephemeral = ephemeral + "PLUN";
        } else if (opr == 58) {
          ephemeral = ephemeral + "MINN";
        } else if (opr == 59) {
          ephemeral = ephemeral + "MULN";
        } else if (opr == 60) {
          ephemeral = ephemeral + "DIVN";
        } else if (opr == 61) {
          ephemeral = ephemeral + "PROB";
        } else if (opr == 62) {
          ephemeral = ephemeral + "STDD";
        } else if (opr == 63) {
          ephemeral = ephemeral + "USER";
        } else {
          /* Such an "else" MAY come into existence if not ALL possible */
          /* instructions have been implemented, either theoretically, or */
          /* practically - simply due to a lack of flash space. */
          ephemeral = ephemeral + "NOKO";
        }
            
        ephemeral = ephemeral + ":";
        ephemeral = ephemeral + datadr1 + ">" + datum[datadr1];
        if ((datum[datadr1] > -1 * DATAMEM) && (datum[datadr1] < DATAMEM)) {
          j = (int) absvalu((int)(datum[datadr1]));
          ephemeral = ephemeral + ">" + datum[j];
        }
        ephemeral = ephemeral + ":";
            
        ephemeral = ephemeral + datadr2 + ">" + datum[datadr2];
        if ((datum[datadr2] > -1 * DATAMEM) && (datum[datadr2] < DATAMEM)) {
          j = (int) absvalu((int)(datum[datadr2]));
          ephemeral = ephemeral + ">" + datum[j];
        }
        ephemeral = ephemeral + ":";
            	
        ephemeral = ephemeral + "DEC:" + instr;
				
		form.append(ephemeral);



        form.addCommand(reentry);          // submit -> reentry
		form.setCommandListener(this);
		
	  } else if (cmdadr == 0) {
		form.deleteAll();
	    // THIS WORKS:
	    ephemeral = Integer.toString(cmdadr) + "   " + Integer.toString(opr) + "   " + Integer.toString(datadr1) + "   " + Integer.toString(datadr2);

        form.append(ephemeral);

        if ((datadr1 >= DATAMEM) || (datadr2 >= DATAMEM) ||
           (datadr1 < 0) || (datadr1 < 0) || (cmdadr >= EXECMEM)) {
		  form.append(" CORRECTING RANGE. DATA 0  ... " + (DATAMEM - 1) + ", COMMANDS 0 ... " + (EXECMEM - 1));
        }
		
        /* only positive data addresses - and 0 - are allowed: */
        datadr1 = (int) absvalu(datadr1);
        datadr2 = (int) absvalu(datadr2);

        /* force everything to be within range: */
        if (cmdadr >= EXECMEM) {
          cmdadr = EXECMEM - 1;
        }
        if (datadr1 >= DATAMEM) {
          datadr1 = DATAMEM - 1;
        }
        if (datadr2 >= DATAMEM) {
          datadr2 = DATAMEM - 1;
        }
		
		/* Having read the instruction, compose it and save it. */
  
        xx = opr & 63;
        yy = datadr1 & 8191;
        zz = datadr2 & 8191;
        instr = (xx << 26) | (yy << 13) | zz;
		cmdadr = 0;
        instruction[cmdadr] = instr;

        ephemeral = " ";
            
        ephemeral = ephemeral + cmdadr + ":" + opr;
            
        ephemeral = ephemeral + "=";
        if (opr == 0) {
          ephemeral = ephemeral + "NOOP";
        } else if (opr == 1) {
          ephemeral = ephemeral + "JUMP";
        } else if (opr == 2) {
          ephemeral = ephemeral + "IADR";
        } else if (opr == 3) {
          ephemeral = ephemeral + "OUTP";
        } else if (opr == 4) {
          ephemeral = ephemeral + "INPT";
        } else if (opr == 5) {
          ephemeral = ephemeral + "SADR";
        } else if (opr == 6) {
          ephemeral = ephemeral + "SVAL";
        } else if (opr == 7) {
          ephemeral = ephemeral + "IAAS";
        } else if (opr == 8) {
          ephemeral = ephemeral + "IVAS";
        } else if (opr == 9) {
          ephemeral = ephemeral + "PLUS";
        } else if (opr == 10) {
          ephemeral = ephemeral + "MINS";
        } else if (opr == 11) {
          ephemeral = ephemeral + "MULS";
        } else if (opr == 12) {
          ephemeral = ephemeral + "DIVS";
        } else if (opr == 13) {
          ephemeral = ephemeral + "POXY";
        } else if (opr == 14) {
          ephemeral = ephemeral + "LOXY";
        } else if (opr == 15) {
          ephemeral = ephemeral + "IFRA";
        } else if (opr == 16) {
          ephemeral = ephemeral + "REMN";
        } else if (opr == 17) {
          ephemeral = ephemeral + "AMNT";
        } else if (opr == 18) {
          ephemeral = ephemeral + "PERD";
        } else if (opr == 19) {
          ephemeral = ephemeral + "PCNT";
        } else if (opr == 20) {
          ephemeral = ephemeral + "SWAP";
        } else if (opr == 21) {
          ephemeral = ephemeral + "FACT";
        } else if (opr == 22) {
          ephemeral = ephemeral + "COPY";
        } else if (opr == 23) {
          ephemeral = ephemeral + "FRIS";
        } else if (opr == 24) {
          ephemeral = ephemeral + "MNMX";
        } else if (opr == 25) {
          ephemeral = ephemeral + "SORT";
        } else if (opr == 26) {
          ephemeral = ephemeral + "CORS";
        } else if (opr == 27) {
          ephemeral = ephemeral + "TURN";
        } else if (opr == 28) {
          ephemeral = ephemeral + "SUMR";
        } else if (opr == 29) {
          ephemeral = ephemeral + "SUSQ";
        } else if (opr == 30) {
          ephemeral = ephemeral + "IXTH";
        } else if (opr == 31) {
          ephemeral = ephemeral + "ABSR";
        } else if (opr == 32) {
          ephemeral = ephemeral + "SQRT";
        } else if (opr == 33) {
          ephemeral = ephemeral + "SQUA";
        } else if (opr == 34) {
          ephemeral = ephemeral + "CBRT";
        } else if (opr == 35) {
          ephemeral = ephemeral + "CUBE";
        } else if (opr == 36) {
          ephemeral = ephemeral + "LNRN";
        } else if (opr == 37) {
          ephemeral = ephemeral + "EXPR";
        } else if (opr == 38) {
          ephemeral = ephemeral + "RADE";
        } else if (opr == 39) {
          ephemeral = ephemeral + "DERA";
        } else if (opr == 40) {
          ephemeral = ephemeral + "SIND";
        } else if (opr == 41) {
          ephemeral = ephemeral + "COSD";
        } else if (opr == 42) {
          ephemeral = ephemeral + "TAND";
        } else if (opr == 43) {
          ephemeral = ephemeral + "ASND";
        } else if (opr == 44) {
          ephemeral = ephemeral + "ACSD";
        } else if (opr == 45) {
          ephemeral = ephemeral + "ATND";
        } else if (opr == 46) {
          ephemeral = ephemeral + "MSTD";
        } else if (opr == 47) {
          ephemeral = ephemeral + "ZERO";
        } else if (opr == 48) {
          ephemeral = ephemeral + "RAND";
        } else if (opr == 49) {
          ephemeral = ephemeral + "RUND";
        } else if (opr == 50) {
          ephemeral = ephemeral + "CEIL";
        } else if (opr == 51) {
          ephemeral = ephemeral + "TANH";
        } else if (opr == 52) {
          ephemeral = ephemeral + "DTNH";
        } else if (opr == 53) {
          ephemeral = ephemeral + "PLUR";
        } else if (opr == 54) {
          ephemeral = ephemeral + "MINR";
        } else if (opr == 55) {
          ephemeral = ephemeral + "MULR";
        } else if (opr == 56) {
          ephemeral = ephemeral + "DIVR";
        } else if (opr == 57) {
          ephemeral = ephemeral + "PLUN";
        } else if (opr == 58) {
          ephemeral = ephemeral + "MINN";
        } else if (opr == 59) {
          ephemeral = ephemeral + "MULN";
        } else if (opr == 60) {
          ephemeral = ephemeral + "DIVN";
        } else if (opr == 61) {
          ephemeral = ephemeral + "PROB";
        } else if (opr == 62) {
          ephemeral = ephemeral + "STDD";
        } else if (opr == 63) {
          ephemeral = ephemeral + "USER";
        } else {
          /* Such an "else" MAY come into existence if not ALL possible */
          /* instructions have been implemented, either theoretically, or */
          /* practically - simply due to a lack of flash space. */
          ephemeral = ephemeral + "NOKO";
        }
            
        ephemeral = ephemeral + ":";
        ephemeral = ephemeral + datadr1 + ">" + datum[datadr1];
        if ((datum[datadr1] > -1 * DATAMEM) && (datum[datadr1] < DATAMEM)) {
          j = (int) absvalu((int)(datum[datadr1]));
          ephemeral = ephemeral + ">" + datum[j];
        }
        ephemeral = ephemeral + ":";
            
        ephemeral = ephemeral + datadr2 + ">" + datum[datadr2];
        if ((datum[datadr2] > -1 * DATAMEM) && (datum[datadr2] < DATAMEM)) {
          j = (int) absvalu((int)(datum[datadr2]));
          ephemeral = ephemeral + ">" + datum[j];
        }
        ephemeral = ephemeral + ":";
            	
        ephemeral = ephemeral + "DEC:" + instr;
				
		form.append(ephemeral);

		form.addCommand(runcode);          // GO: submit -> runcode"SUBMIT TO RUN CODE EXIT TO CANCEL"
		form.append(" ENTER TO RUN, EXIT TO ABORT");
		form.setCommandListener(this);
	  } else if (cmdadr == -1) {
		form.deleteAll();
		form.append(d1_field);
		form.append(d2_field);
		form.append(" LIST INSTRUCTION RANGE");
		form.addCommand(setvalue);
		form.setCommandListener(this);
	  } else if (cmdadr == -2) {
		form.deleteAll();
		form.append(d1_field);
		form.append(d2_field);
		form.append(" LIST DATA RANGE");
		form.addCommand(setvalue);
		form.setCommandListener(this);
	  } else if (cmdadr == -3) {
		form.deleteAll();
		form.append(d1_field);
		form.append(" ENTER INSTRUCTIONS FROM ADDRESS UP");
		form.addCommand(setvalue);
		form.setCommandListener(this);
	  } else if (cmdadr == -4) {
		form.deleteAll();
		form.append(d1_field);
		form.append(d2_field);
		form.append(" ENTER NUMBERS IN DATA RANGE");
		form.addCommand(setvalue);
		form.setCommandListener(this);
	  } else if (cmdadr == -5) {
		form.deleteAll();
		form.append(d1_field);
		form.append(d2_field);
		form.append(" CLEAR INSTRUCTION RANGE");
		form.addCommand(setvalue);
		form.setCommandListener(this);
	  } else if (cmdadr == -6) {
		form.deleteAll();
		form.append(d1_field);
		form.append(d2_field);
		form.append(" CLEAR DATA RANGE");
		form.addCommand(setvalue);
		form.setCommandListener(this);
	  } else if (cmdadr == -7) {
		form.deleteAll();
		form.addCommand(reentry);
		form.setCommandListener(this);
	    form.append(" TRACE ON");
        tracer = 1;
	  } else if (cmdadr == -8) {
		form.addCommand(reentry);
		form.setCommandListener(this);
	    form.append(" TRACE OFF");
        tracer = 0;
	  } else if (cmdadr == -9) {
		form.deleteAll();
		form.append(rl_field);
		form.addCommand(setvalue);
		form.setCommandListener(this);
      } else if (cmdadr == -10) {
		form.deleteAll();
		form.addCommand(reentry);          // EJECT: -> reentry
		form.setCommandListener(this);
		form.append(" EXIT FROM INIT MENU");
	  } else {
		form.deleteAll();
		form.addCommand(reentry);          // EJECT: -> reentry
		form.setCommandListener(this);
		form.append(" COMMAND NOT AVAILABLE");
	  }
	  
	} else if (command == reentry) {
	  previouscommand = command;
	  form.deleteAll();                  // CLS at first safe place
      form.removeCommand(reentry);       // cycle the available commands
	  form.removeCommand(runcode);       // cycle the available commands
	                                     // Better do stuff BEFORE showing controls.
	  pc = 0;                            // safety: nuke the program counter and the immediate instruction
      instruction[0] = 0;

      form.setCommandListener(this);
	  form.addCommand(submit);           // reentry -> submit
	  form.append(cmdfield);             // REDRAW EVERYTHING
	  form.append(oprfield);
      form.append(d1_field);
	  form.append(d2_field);
	  form.setCommandListener(this);
	  cmdfield.setString("0");
	  oprfield.setString("0");
	  d1_field.setString("1");
	  d2_field.setString("1");

	} else if (command == setvalue) {
	  previouscommand = setvalue;
	  form.removeCommand(setvalue);
	  
	  if (entryflag == -3) {
		form.deleteAll();
	    ephemeral = intfield.getString();
	    instr = java.lang.Integer.parseInt(ephemeral);
		
		if ((instr == 67108863) || (datadr1 == EXECMEM)) { /* eject */
			entryflag = 0;
			form.addCommand(reentry);
			form.setCommandListener(this);
			form.append(" INSTRUCTION ENTRY DONE");
			cmdadr = 0;
			opr = 0;
			datadr1 = 0;
			datadr2 = 0;
		} else {
			instruction[datadr1] = instr;
			form.append(intfield);
			intfield.setString("0");
			form.addCommand(setvalue);
			form.setCommandListener(this);
			form.append(" ADDRESS: " + datadr1 + ", OPERATION:" + instr + ", ENTER NEXT OPERATION OR 67108863");
			datadr1++;
		}

	  } else if (entryflag == -4) {
		form.deleteAll();
	    ephemeral = flofield.getString();
	    f = java.lang.Float.parseFloat(ephemeral);
		datum[datadr1] = f;
		datadr1++;
		if (datadr1 > datadr2) {
			entryflag = 0;
			form.addCommand(reentry);
			form.setCommandListener(this);
			form.append(" ADDRESS: " + (datadr1 - 1) + ", DATUM:" + f + ", DONE");
			cmdadr = 0;
			opr = 0;
			datadr1 = 0;
			datadr2 = 0;
		} else {
			form.append(flofield);
			flofield.setString("0");
			form.addCommand(setvalue);
			form.setCommandListener(this);
			form.append(" ADDRESS: " + (datadr1 - 1) + ", DATUM:" + f + ", ENTER NEXT DATUM");
		}

	  } else if (cmdadr == -1) {
	    form.deleteAll();

		ephemeral = d1_field.getString();
	    datadr1 = java.lang.Integer.parseInt(ephemeral);
	    ephemeral = d2_field.getString();
	    datadr2 = java.lang.Integer.parseInt(ephemeral);

		if ((datadr1 < 0) || (datadr2 < 0)) {                   // fast eject - do nothing
			form.append(" CANCELLED ON NEGATIVE RANGE");
		} else {

			if ((datadr1 == 0) && (datadr2 == 0)) {
			/* print EVERYTHING */
				datadr1 = 1;
				datadr2 = DATAMEM - 1;
			} else if (datadr2 == 0) {
				datadr2 = datadr1; /* i.e. print only one datum */
			}

			/* Normalise ranges: low to high */
			if (datadr2 < datadr1) {
				k = datadr2;
				datadr2 = datadr1;
				datadr1 = k;
			}

			h = datadr1;
			k = datadr2;
			if (h < 1) { h = 1;}
			if (k >= DATAMEM) { k = DATAMEM - 1; }

			for (i = h; i <= k; i++) {

                instr = instruction[i];
                // EEPROM.get((INSTRUCTION_BYTE_LENGTH * (i - 0)) + ZERO_OFFSET, instr);
                xx = (instr >> 26) & 63; /* 4095:255:24:12 or 8191:63:26:13 */
                yy = (instr >> 13) & 8191;
                zz = instr & 8191;
                opr = xx;
                datadr1 = yy;
                datadr2 = zz;
                ephemeral = " ";
            
                ephemeral = ephemeral + i + ":" + opr;
            
                ephemeral = ephemeral + "=";
                if (opr == 0) {
                  ephemeral = ephemeral + "NOOP";
                } else if (opr == 1) {
                  ephemeral = ephemeral + "JUMP";
                } else if (opr == 2) {
                  ephemeral = ephemeral + "IADR";
                } else if (opr == 3) {
                  ephemeral = ephemeral + "OUTP";
                } else if (opr == 4) {
                  ephemeral = ephemeral + "INPT";
                } else if (opr == 5) {
                  ephemeral = ephemeral + "SADR";
                } else if (opr == 6) {
                  ephemeral = ephemeral + "SVAL";
                } else if (opr == 7) {
                  ephemeral = ephemeral + "IAAS";
                } else if (opr == 8) {
                  ephemeral = ephemeral + "IVAS";
                } else if (opr == 9) {
                  ephemeral = ephemeral + "PLUS";
                } else if (opr == 10) {
                  ephemeral = ephemeral + "MINS";
                } else if (opr == 11) {
                  ephemeral = ephemeral + "MULS";
                } else if (opr == 12) {
                  ephemeral = ephemeral + "DIVS";
                } else if (opr == 13) {
                  ephemeral = ephemeral + "POXY";
                } else if (opr == 14) {
                  ephemeral = ephemeral + "LOXY";
                } else if (opr == 15) {
                  ephemeral = ephemeral + "IFRA";
                } else if (opr == 16) {
                  ephemeral = ephemeral + "REMN";
                } else if (opr == 17) {
                  ephemeral = ephemeral + "AMNT";
                } else if (opr == 18) {
                  ephemeral = ephemeral + "PERD";
                } else if (opr == 19) {
                  ephemeral = ephemeral + "PCNT";
                } else if (opr == 20) {
                  ephemeral = ephemeral + "SWAP";
                } else if (opr == 21) {
                  ephemeral = ephemeral + "FACT";
                } else if (opr == 22) {
                  ephemeral = ephemeral + "COPY";
                } else if (opr == 23) {
                  ephemeral = ephemeral + "FRIS";
                } else if (opr == 24) {
                  ephemeral = ephemeral + "MNMX";
                } else if (opr == 25) {
                  ephemeral = ephemeral + "SORT";
                } else if (opr == 26) {
                  ephemeral = ephemeral + "CORS";
                } else if (opr == 27) {
                  ephemeral = ephemeral + "TURN";
                } else if (opr == 28) {
                  ephemeral = ephemeral + "SUMR";
                } else if (opr == 29) {
                  ephemeral = ephemeral + "SUSQ";
                } else if (opr == 30) {
                  ephemeral = ephemeral + "IXTH";
                } else if (opr == 31) {
                  ephemeral = ephemeral + "ABSR";
                } else if (opr == 32) {
                  ephemeral = ephemeral + "SQRT";
                } else if (opr == 33) {
                  ephemeral = ephemeral + "SQUA";
                } else if (opr == 34) {
                  ephemeral = ephemeral + "CBRT";
                } else if (opr == 35) {
                  ephemeral = ephemeral + "CUBE";
                } else if (opr == 36) {
                  ephemeral = ephemeral + "LNRN";
                } else if (opr == 37) {
                  ephemeral = ephemeral + "EXPR";
                } else if (opr == 38) {
                  ephemeral = ephemeral + "RADE";
                } else if (opr == 39) {
                  ephemeral = ephemeral + "DERA";
                } else if (opr == 40) {
                  ephemeral = ephemeral + "SIND";
                } else if (opr == 41) {
                  ephemeral = ephemeral + "COSD";
                } else if (opr == 42) {
                  ephemeral = ephemeral + "TAND";
                } else if (opr == 43) {
                  ephemeral = ephemeral + "ASND";
                } else if (opr == 44) {
                  ephemeral = ephemeral + "ACSD";
                } else if (opr == 45) {
                  ephemeral = ephemeral + "ATND";
                } else if (opr == 46) {
                  ephemeral = ephemeral + "MSTD";
                } else if (opr == 47) {
                  ephemeral = ephemeral + "ZERO";
                } else if (opr == 48) {
                  ephemeral = ephemeral + "RAND";
                } else if (opr == 49) {
                  ephemeral = ephemeral + "RUND";
                } else if (opr == 50) {
                  ephemeral = ephemeral + "CEIL";
                } else if (opr == 51) {
                  ephemeral = ephemeral + "TANH";
                } else if (opr == 52) {
                  ephemeral = ephemeral + "DTNH";
                } else if (opr == 53) {
                  ephemeral = ephemeral + "PLUR";
                } else if (opr == 54) {
                  ephemeral = ephemeral + "MINR";
                } else if (opr == 55) {
                  ephemeral = ephemeral + "MULR";
                } else if (opr == 56) {
                  ephemeral = ephemeral + "DIVR";
                } else if (opr == 57) {
                  ephemeral = ephemeral + "PLUN";
                } else if (opr == 58) {
                  ephemeral = ephemeral + "MINN";
                } else if (opr == 59) {
                  ephemeral = ephemeral + "MULN";
                } else if (opr == 60) {
                  ephemeral = ephemeral + "DIVN";
                } else if (opr == 61) {
                  ephemeral = ephemeral + "PROB";
                } else if (opr == 62) {
                  ephemeral = ephemeral + "STDD";
                } else if (opr == 63) {
                  ephemeral = ephemeral + "USER";
                } else {
                  /* Such an "else" MAY come into existence if not ALL possible */
                  /* instructions have been implemented, either theoretically, or */
                  /* practically - simply due to a lack of flash space. */
                  ephemeral = ephemeral + "NOKO";
                }
            
                ephemeral = ephemeral + ":";
                ephemeral = ephemeral + datadr1 + ">" + datum[datadr1];
                if ((datum[datadr1] > -1 * DATAMEM) && (datum[datadr1] < DATAMEM)) {
                  j = (int) absvalu((int)(datum[datadr1]));
                  ephemeral = ephemeral + ">" + datum[j];
                }
                ephemeral = ephemeral + ":";
            
                ephemeral = ephemeral + datadr2 + ">" + datum[datadr2];
                if ((datum[datadr2] > -1 * DATAMEM) && (datum[datadr2] < DATAMEM)) {
                  j = (int) absvalu((int)(datum[datadr2]));
                  ephemeral = ephemeral + ">" + datum[j];
                }
                ephemeral = ephemeral + ":";
            	
            	ephemeral = ephemeral + "DEC:" + instr;
				
				form.append(ephemeral);

			}

			h = 0;
			k = 0;
			i = 0;
		}

	  } else if (cmdadr == -2) {
	    form.deleteAll();
		
		ephemeral = d1_field.getString();
	    datadr1 = java.lang.Integer.parseInt(ephemeral);
	    ephemeral = d2_field.getString();
	    datadr2 = java.lang.Integer.parseInt(ephemeral);

		if ((datadr1 < 0) || (datadr2 < 0)) {                   // fast eject - do nothing
			form.append(" CANCELLED ON NEGATIVE RANGE");
		} else {

			if ((datadr1 == 0) && (datadr2 == 0)) {
			/* print EVERYTHING */
				datadr1 = 1;
				datadr2 = DATAMEM - 1;
			} else if (datadr2 == 0) {
				datadr2 = datadr1; /* i.e. print only one datum */
			}

			/* Normalise ranges: low to high */
			if (datadr2 < datadr1) {
				k = datadr2;
				datadr2 = datadr1;
				datadr1 = k;
			}

			h = datadr1;
			k = datadr2;
			if (h < 1) { h = 1;}
			if (k >= DATAMEM) { k = DATAMEM - 1; }

			for (i = h; i <= k; i++) {
				ephemeral = " " + i + ":" + datum[i];
				form.append(ephemeral);
			}

			h = 0;
			k = 0;
			i = 0;
		
		}
		
	  } else if (cmdadr == -3) {
	    form.deleteAll();
	    ephemeral = d1_field.getString();
	    datadr1 = java.lang.Integer.parseInt(ephemeral);

		if ((datadr1 < 0) || (datadr1 > EXECMEM - 1)) {                                 // fast eject - do nothing
			form.append(" CANCELLED ON OUT OF RANGE COMMAND ADDRESS");
		} else {
			form.append(intfield);
			intfield.setString("0");
			form.addCommand(setvalue);
			form.setCommandListener(this);
			entryflag = -3;
		}
  
	  } else if (cmdadr == -4) {
	    form.deleteAll();
	    ephemeral = d1_field.getString();
	    datadr1 = java.lang.Integer.parseInt(ephemeral);
	    ephemeral = d2_field.getString();
	    datadr2 = java.lang.Integer.parseInt(ephemeral);

		if ((datadr1 < 0) || (datadr2 < 0) || (datadr2 < datadr1)) {                   // fast eject - do nothing
			form.append(" CANCELLED ON NEGATIVE RANGE OR DESCENDING ADDRESSES");

		} else {
			form.append(flofield);
			flofield.setString("0");
			form.addCommand(setvalue);
			form.setCommandListener(this);
			entryflag = -4;
		}
		  
	  } else if (cmdadr == -5) {
	    form.deleteAll();
		
	    ephemeral = d1_field.getString();
	    datadr1 = java.lang.Integer.parseInt(ephemeral);
	    ephemeral = d2_field.getString();
	    datadr2 = java.lang.Integer.parseInt(ephemeral);

		if ((datadr1 < 0) || (datadr2 < 0)) {                   // fast eject - do nothing
			form.append(" CANCELLED ON NEGATIVE RANGE");
		} else {



			if ((datadr1 == 0) && (datadr2 == 0)) {
				/* clear EVERYTHING */
				datadr1 = 1;
				datadr2 = DATAMEM - 1;
			} else if (datadr2 == 0) {
				datadr2 = datadr1; /* clear one single instruction */
			}

			/* Normalise ranges: low to high */
			if (datadr2 < datadr1) {
				k = datadr2;
				datadr2 = datadr1;
				datadr1 = k;
			}

			h = datadr1;
			k = datadr2;
			if (h < 1) { h = 1;} /* Do not clear address 0, it is reserved. */
			if (k >= EXECMEM) { k = EXECMEM - 1; } /* out-of-range-correction */

			instr = 0; /* Just to make sure it "understands" the size as 32bit. */
			for (i = h; i <= k; i++) {
				instruction[i] = instr;
			}
			form.append(" INSTRUCTIONS CLEARED");

			h = 0;
			k = 0;
			i = 0;
		}
		  
	  } else if (cmdadr == -6) {
	    form.deleteAll();
		
	    ephemeral = d1_field.getString();
	    datadr1 = java.lang.Integer.parseInt(ephemeral);
	    ephemeral = d2_field.getString();
	    datadr2 = java.lang.Integer.parseInt(ephemeral);

		if ((datadr1 < 0) || (datadr2 < 0)) {                   // fast eject - do nothing
			form.append(" CANCELLED ON NEGATIVE RANGE");
		} else {

			if ((datadr1 == 0) && (datadr2 == 0)) {
			/* clear EVERYTHING */
				datadr1 = 1;
				datadr2 = DATAMEM - 1;
			} else if (datadr2 == 0) {
				datadr2 = datadr1; /* i.e. clear only one datum */
			}

			/* Normalise ranges: low to high */
			if (datadr2 < datadr1) {
				k = datadr2;
				datadr2 = datadr1;
				datadr1 = k;
			}

			h = datadr1;
			k = datadr2;
			if (h < 1) { h = 1;}
			if (k >= DATAMEM) { k = DATAMEM - 1; }

			for (i = h; i <= k; i++) {
				datum[i] = 0.0f;
			}
			form.append(" DATA CLEARED");

			h = 0;
			k = 0;
			i = 0;
		
		}
	  
	  } else if (cmdadr == -9) {
	    form.deleteAll();
		ephemeral = "OLD RUNLIMIT WAS " + runlimit;
		form.append(ephemeral);
	    ephemeral = rl_field.getString();
	    runlimit = java.lang.Integer.parseInt(ephemeral);
		form.append(" NEW RUNLIMIT SET TO " + ephemeral);
	  }
	  
	  if (entryflag == 0) {
		form.addCommand(reentry);          // setvalue -> reentry
		form.setCommandListener(this);
	  } else {
		form.addCommand(setvalue);
		form.setCommandListener(this);
	  }
	  
	} else if (command == runcode) {
	  form.deleteAll();
	  previouscommand = command;
	  form.removeCommand(runcode);
	  form.removeCommand(submit);
	  
	  form.append(" RUNNING JOB...");

	  form.addCommand(reentry);          // runcode -> reentry
	  form.setCommandListener(this);




      while (pc < EXECMEM) {


		if (runlimit == 1) {
			rlstring = "TERMINATED WITH RUNLIMIT EXHAUSTED, PC=" + pc + " STOPPED";
			break;
		} else if (runlimit > 1) {
			runlimit--;
			if (((runlimit - 1) % 1000) == 0) {
				form.append(" RUNLIMIT = " + runlimit);
				/* Give an indication of progress without annoying too often. */
			}
			rlstring = "";
		}

		datum[0] = 0.0f; /* Useful for unconditional jumps. */

		if (pc < 0) {
			pc = 0;
		}

System.out.println("X CMDADR = " + cmdadr + " PC = " + pc + " OPR=" + opr + " DATADR1=" + datadr1 + " DATADR2=" + datadr2 );

		instr = instruction[pc];
System.out.println("DEC INSTRUCTION:" + instr);

		/* 63 operations possible, on 8191 places of data for each address; 26,13 */
		/* or: 255 operations, on 4095 data places for each address; 24,12 */
		opr = (instr >> 26) & 63;
		datadr1 = (instr >> 13) & 8191;
		datadr2 = instr & 8191;

		/* Is an instruction modifier active? If yes, change each dataddr. */
		/* This is due to IADR, indirect addressing, see below opcode 2. */
		if ((h > 0) && (k > 0)) {
			datadr1 = h;
			datadr2 = k;
			h = 0;
			k = 0;
		}

System.out.println("Y CMDADR = " + cmdadr + " PC = " + pc + " OPR=" + opr + " DATADR1=" + datadr1 + " DATADR2=" + datadr2 );

		/* TRACE EXECUTION OF EACH PRESENT INSTRUCTION */
		if (tracer != 0) {
			trace1s = " ";
		
			trace1s = trace1s + pc + ":" + opr;
		
			trace1s = trace1s + "=";
			if (opr == 0) {
			  trace1s = trace1s + "NOOP";
			} else if (opr == 1) {
			  trace1s = trace1s + "JUMP";
			} else if (opr == 2) {
			  trace1s = trace1s + "IADR";
			} else if (opr == 3) {
			  trace1s = trace1s + "OUTP";
			} else if (opr == 4) {
			  trace1s = trace1s + "INPT";
			} else if (opr == 5) {
			  trace1s = trace1s + "SADR";
			} else if (opr == 6) {
			  trace1s = trace1s + "SVAL";
			} else if (opr == 7) {
			  trace1s = trace1s + "IAAS";
			} else if (opr == 8) {
			  trace1s = trace1s + "IVAS";
			} else if (opr == 9) {
			  trace1s = trace1s + "PLUS";
			} else if (opr == 10) {
			  trace1s = trace1s + "MINS";
			} else if (opr == 11) {
			  trace1s = trace1s + "MULS";
			} else if (opr == 12) {
			  trace1s = trace1s + "DIVS";
			} else if (opr == 13) {
			  trace1s = trace1s + "POXY";
			} else if (opr == 14) {
			  trace1s = trace1s + "LOXY";
			} else if (opr == 15) {
			  trace1s = trace1s + "IFRA";
			} else if (opr == 16) {
			  trace1s = trace1s + "REMN";
			} else if (opr == 17) {
			  trace1s = trace1s + "AMNT";
			} else if (opr == 18) {
			  trace1s = trace1s + "PERD";
			} else if (opr == 19) {
			  trace1s = trace1s + "PCNT";
			} else if (opr == 20) {
			  trace1s = trace1s + "SWAP";
			} else if (opr == 21) {
			  trace1s = trace1s + "FACT";
			} else if (opr == 22) {
			  trace1s = trace1s + "COPY";
			} else if (opr == 23) {
			  trace1s = trace1s + "FRIS";
			} else if (opr == 24) {
			  trace1s = trace1s + "MNMX";
			} else if (opr == 25) {
			  trace1s = trace1s + "SORT";
			} else if (opr == 26) {
			  trace1s = trace1s + "CORS";
			} else if (opr == 27) {
			  trace1s = trace1s + "TURN";
			} else if (opr == 28) {
			  trace1s = trace1s + "SUMR";
			} else if (opr == 29) {
			  trace1s = trace1s + "SUSQ";
			} else if (opr == 30) {
			  trace1s = trace1s + "IXTH";
			} else if (opr == 31) {
			  trace1s = trace1s + "ABSR";
			} else if (opr == 32) {
			  trace1s = trace1s + "SQRT";
			} else if (opr == 33) {
			  trace1s = trace1s + "SQUA";
			} else if (opr == 34) {
			  trace1s = trace1s + "CBRT";
			} else if (opr == 35) {
			  trace1s = trace1s + "CUBE";
			} else if (opr == 36) {
			  trace1s = trace1s + "LNRN";
			} else if (opr == 37) {
			  trace1s = trace1s + "EXPR";
			} else if (opr == 38) {
			  trace1s = trace1s + "RADE";
			} else if (opr == 39) {
			  trace1s = trace1s + "DERA";
			} else if (opr == 40) {
			  trace1s = trace1s + "SIND";
			} else if (opr == 41) {
			  trace1s = trace1s + "COSD";
			} else if (opr == 42) {
			  trace1s = trace1s + "TAND";
			} else if (opr == 43) {
			  trace1s = trace1s + "ASND";
			} else if (opr == 44) {
			  trace1s = trace1s + "ACSD";
			} else if (opr == 45) {
			  trace1s = trace1s + "ATND";
			} else if (opr == 46) {
			  trace1s = trace1s + "MSTD";
			} else if (opr == 47) {
			  trace1s = trace1s + "ZERO";
			} else if (opr == 48) {
			  trace1s = trace1s + "RAND";
			} else if (opr == 49) {
			  trace1s = trace1s + "RUND";
			} else if (opr == 50) {
			  trace1s = trace1s + "CEIL";
			} else if (opr == 51) {
			  trace1s = trace1s + "TANH";
			} else if (opr == 52) {
			  trace1s = trace1s + "DTNH";
			} else if (opr == 53) {
			  trace1s = trace1s + "PLUR";
			} else if (opr == 54) {
			  trace1s = trace1s + "MINR";
			} else if (opr == 55) {
			  trace1s = trace1s + "MULR";
			} else if (opr == 56) {
			  trace1s = trace1s + "DIVR";
			} else if (opr == 57) {
			  trace1s = trace1s + "PLUN";
			} else if (opr == 58) {
			  trace1s = trace1s + "MINN";
			} else if (opr == 59) {
			  trace1s = trace1s + "MULN";
			} else if (opr == 60) {
			  trace1s = trace1s + "DIVN";
			} else if (opr == 61) {
			  trace1s = trace1s + "PROB";
			} else if (opr == 62) {
			  trace1s = trace1s + "STDD";
			} else if (opr == 63) {
			  trace1s = trace1s + "USER";
			} else {
			  /* Such an "else" MAY come into existence if not ALL possible */
			  /* instructions have been implemented, either theoretically, or */
			  /* practically - simply due to a lack of flash space. */
			  trace1s = trace1s + "NOKO";
			}
		
			trace1s = trace1s + ":";
			trace1s = trace1s + datadr1 + ">" + datum[datadr1];
			if ((datum[datadr1] > -1 * DATAMEM) && (datum[datadr1] < DATAMEM)) {
			  j = (int) absvalu((int)(datum[datadr1]));
			  trace1s = trace1s + ">" + datum[j];
			}
			trace1s = trace1s + ":";
		
			trace1s = trace1s + datadr2 + ">" + datum[datadr2];
			if ((datum[datadr2] > -1 * DATAMEM) && (datum[datadr2] < DATAMEM)) {
			  j = (int) absvalu((int)(datum[datadr2]));
			  trace1s = trace1s + ">" + datum[j];
			}
			trace1s = trace1s + ":";
			
			trace1s = trace1s + "DEC:" + instr;
			
		} /* end of instruction tracing */






		  /* ---------- DECODE ---------- */
		  /* Quite simply: "do different things depending on what operation (opr) */
		  /* is presently indicated by the program counter (pc). */
		  /* There are two instructions which deal with program CONTROL, and */
		  /* all other instructions deal with reading, writing and storing values */
		  /* as well as with mathematical operations, which are indeed the focus. */
		  /* The one is to jump if at a certain address the value is (close to) */
		  /* zero or negative - and this is the ONLY branch instruction, and thus */
		  /* loops are to be implemented by "jumping backwards" and "reducing the */
		  /* addressed value" - and the other instruction sets datadr1 and datadr2 */
		  /* to be overwritten by certain values upon the next instruction; this */
		  /* should allow rather flexible programming. - This section "DECODE" it */
		  /* is where you may really adjust this whole system to your taste: */
		  /* shuffle opr-values, reduce or extend or modify the instructions, etc. */

		  /* WHATEVER IS NOT MATCHED: NO OPERATION [NOP] / RESERVED */
		  /* This particularly applies to "Operation 0", so all starts with 1. */

		  if (opr == 0) {
		  /* NOOP -- NO OPERATION: Do nothing. */
		  /* Any non-implemented opcode will work as NOP. */
		  /* This is also nice if you want to temporarily de-activate certain code. */

		  } else if (opr == 1) {
		  /* JUMP */
		  /* JUMP IF THE FIRST ADDRESS CONTAINS (NEARLY) ZERO OR A NEGATIVE NUMBER*/
		  /* "Nearly" zero, because these are floats and repeated float operations */
		  /* may render the results imprecise. */
		  /* This can very neatly be translated into a high language's IF & WHILE. */
		  /* Anyway, welcome to "spaghetti programming". You will use this sole */
		  /* condition until you learn to LOVE IT! */

			if (datum[datadr1] <= TOLERANCE) {
			  /* pc = (int) absvalu((int)(datum[datadr2])); */ /* However, such a default is */
			  /* NOT a good idea, actually: with a default COMPUTED GOTO, you */
			  /* normally straight GOTO HELL, and have NO adequate way to trace it. */
			  /* Instead, let the default be OBVIOUS: */

			  pc = datadr2; 
			  /* On purpose NOT "computed", but IADR still allows it. */
			  /* BEWARE: the program counter adjustment below needed to be RESET, */
			  /* otherwise it was sending you ONE AFTER your desired jump address! */
			} else { pc++; } /* without this, it gets stuck! */
			/* This pc adjustment above makes jumps NOT return to immediate mode: */
			/* pc is set to, essentially, an integer indicated by the float value */
			/* at an address. But a return to immediate mode is only possible if */
			/* pc stays 0. This is why a jump can serve as an "entry" to exection. */

		  } else if (opr == 2) {
		  /* IADR -- INDIRECT ADDRESSING: */
		  /* SUBSTITUTE datadr1 AND datadr2 IN THE NEXT INSTRUCTION BY h AND k. */
		  /* Whatever the next operation is, datum[datadr1] shall give */
		  /* the next datadr1, and datum[datadr2] shall give the next datadr2. */
		  /* This shall serve for a rather powerful "indirect addressing" */
		  /* whenever such is needed, in a flexible manner. */

			h = (int) absvalu((int)(datum[datadr1]));
			k = (int) absvalu((int)(datum[datadr2]));
			/* Thereby, h & k are "charged" and will trigger a modification of */
			/* the next instruction, substituting its addresses one time. */
			/* You can obviously cancel this with a NOP. */

			/* Make sure the addresses are within the allowed address range. */
			if (h < 1) {
			  h = 1;
			}

			if (k < 1) {
			  k = 1;
			}

			if (h >= DATAMEM) {
			  h = DATAMEM - 1;
			}

			if (k >= DATAMEM) {
			  k = DATAMEM - 1;
			}

		  /* ----------- FROM NOW ON FOLLOW COMMANDS FOR SETTING VALUES ----------- */
		  } else if (opr == 3) {
		  /* OUTP -- write 0 for OUTPUT if you have a numeric display. */
		  /* This instruction will print all data in a range, unless the second */
		  /* address is seither the same as the first address or 0, indicating */
		  /* that only the first address is to be printed. */

          /* NO NERVE FOR THIS MIDLET TRASH, USE IT AS A "PUNCH CARD MACHINE"! */

		  } else if (opr == 4) {
		  /* INPT -- write 1 for INPUT if you have a numeric display. */
		  /* This instruction will read a range of numbers, unless */
		  /* the second address is the same as the first address or 0, */
		  /* indicating that only a single number is to be read. */

          /* NO NERVE FOR THIS MIDLET TRASH, USE IT AS A "PUNCH CARD MACHINE"! */

		  } else if (opr == 5) {
		  /* SADR -- SET ADDRESS AS VALUE: */
		  /* This can also used to let an address be treated as a value in some */
		  /* future instruction - IADR (opcode 2) can help reversing the process. */

			datum[datadr1] = 0.0f + datadr2;
			/* obviously, if datadr2 == datadr1, this will make an address */
			/* "hold itself" */

		  } else if (opr == 6) {
		  /* SVAL -- SET VALUE: This is the "normal 'equal'" in other languages. */
		  /* Did not call it EQUL or thelike, to help visually differentiate */
		  /* values and addresses. */
			datum[datadr1] = datum[datadr2];

		  } else if (opr == 7) {
		  /* HOW IxAS WORKS: */
		  /* datadr1 contains an address X, i.e. it points to it; */
		  /* X is at first expressed as a float to be converted to an integer. */
		  /* datum[X] shall either be set to an address or a value that is defined */
		  /* in datadr2, i.e. datadr2 itself or the datum at datadr2, respectively. */
		  /* In other words, datum[datum[datadr1]] = datadr2, from now on */
		  /* Indirect Address Assignment or IAAS, or Indirect Value Assignemnt */
		  /* IVAS signifying datum[datum[datadr1]] = datum[datadr2]. These may look */
		  /* pretty strange, but they appeared practical for certain loops. */ 

		  /* IAAS */
			i = (int) absvalu((int)(datum[datadr1]));
			datum[i] = 0.0f + datadr2;

		  } else if (opr == 8) {
		  /* IVAS -- see above. */

			i = (int) absvalu((int)(datum[datadr1]));
			datum[i] = datum[datadr2];

		  /* ---- FROM NOW ON FOLLOW COMMANDS FOR SINGLE NUMERIC MANIPULATION ----- */
		  } else if (opr == 9) {
		  /* PLUS -- PLUS FOR SINGLE NUMBERS, NOT RANGE. */

			datum[datadr1] = datum[datadr1] + datum[datadr2];

		  } else if (opr == 10) {
		  /* MINS -- MINUS FOR SINGLE NUMBERS, NOT RANGE. */

			datum[datadr1] = datum[datadr1] - datum[datadr2];

		  } else if (opr == 11) {
		  /* MULS -- MULTIPLY SINGLE NUMBERS, NOT RANGE. */

			datum[datadr1] = datum[datadr1] * datum[datadr2];

		  } else if (opr == 12) {
		  /* DIVS -- DIVIDE SINGLE NUMBERS, NOT RANGE. */
		  /* In case of a division by 0, there shall be no hysteria. Just give 0. */
		  /* This makes loops much easier than "having to look out for the */
		  /* mathematical booby-trap" all the time. Here this is done GENERALLY. */

			if (absvalu(datum[datadr2]) != 0.0f) {
			  datum[datadr1] = datum[datadr1] / datum[datadr2];
			} else {
			  datum[datadr1] = 0.0f;
			}

		  } else if (opr == 13) {
		  /* POXY -- Power Of X: Y, that is, X ^ Y, BASED ON |X| WITH SIGN EX POST. */
		  /* This is a rather powerful instruction, because 1/Y is the Yth root */
		  /* (so Y = 0.5 is a square root, Y= 0.3333 is a cube root, and so on),
		  /* Y = -1 sets X to 1/X, and so forth. - All this operates on the */
		  /* ABSOLUTE VALUE OF X, and preserving the sign, to prevent imaginary */
		  /* number results. Unfortunately, this has the funny effect that -3^2=-9. */

			/* A ^ B =  E ^ (B * ln(A)), but that is not even necessary due to pow. */
			if (datum[datadr1] == 0.0f) {
			  datum[datadr1] = 0.0f;
			  /* Wanton value because it is undefined for negative exponents */
			  /* and for positive exponents, 0^whatever=0. */
			} else if (datum[datadr1] < 0.0f) {
			  datum[datadr1] = -1.0f * (float) pow(absvalu(datum[datadr1]), datum[datadr2]);
			} else {
			  datum[datadr1] = (float) pow(datum[datadr1], datum[datadr2]);
			}

		  } else if (opr == 14) {
		  /* LOXY -- LogY X, i.e. of X to the base of Y. X needs not be an integer. */
		  /* This is based on the effect of loga (b) = ln (b) / ln (a), */
		  /* b is in datadr1, a is in datadr2, and both are forced to be positive. */
		  /* If any of the two numbers is 0.0, the result is set to be 0.0, too. */
		  
		    /* OMITTED - DYSFUNCTIONAL 
			if ((absvalu(datum[datadr1]) != 0) && (absvalu(datum[datadr2]) != 0)) {
			  datum[datadr1] = (float) log(absvalu(datum[datadr1])) / (float) log(absvalu(datum[datadr2]));
			} else {
			  datum[datadr1] = 0.0f;
			}
			*/

		  } else if (opr == 15) {
		  /* IFRA -- Integral and Fractional part of a number stored in datadr1. */

			g = datum[datadr1];
			datum[datadr2] = g % 1; /* the fractional part */
			datum[datadr1] = g - datum[datadr1]; /* the integral part */

		  } else if (opr == 16) {
		  /* REMN -- Remainer of the division between datum[datadr1] and */
		  /* datum[datadr2], whereby 0 is assumed in case of division by 0. */
			if (absvalu(datum[datadr2]) != 0.0f) {
			  datum[datadr1] = datum[datadr1] % datum[datadr2];
			} else {
			  datum[datadr1] = 0.0f;
			}

		  } else if (opr == 17) {
		  /* NEXT THREE INSTRUCTIONS: PERCENT CALCULATIONS OVER TIME. */
		  /* Each instruction is named for what is SOUGHT: */
		  /* amount, period or percentage, assuming that */
		  /* amount = (1 + (percentage / 100)) ^ period. */

		  /* AMNT -- Find the amount, whereby */
		  /* datadr1 contains the periods */
		  /* datadr2 contains the percent (not as, say, 0.25, but as 25). */

			f = datum[datadr1];
			g = datum[datadr2];
			g = (float) pow(1 + (g / 100), f);
			datum[datadr1] = g;
			/* you overwrite the period, but keep the percent untouched */
			f = 0.0f;
			g = 0.0f;

		  } else if (opr == 18) {
		  /* PERD -- Find the period, whereby */
		  /* datadr1 contains the final amount */
		  /* datadr2 contains the percent. */

            /* OMITTED - DYSFUNCTIONAL 
			f = datum[datadr1];
			g = datum[datadr2];
			g = (1 + (g / 100));
			g = (float) log(g);
			f = (float) log(f);
			g = f / g;
			datum[datadr1] = g;
			// you overwrite the final amount, but keep the percent untouched
			f = 0.0f;
			g = 0.0f;
			*/

		  } else if (opr == 19) {
		  /* PCNT -- Find the percent, whereby */
		  /* datadr1 contains the final amount */
		  /* datadr2 contains the period */

			f = datum[datadr1];
			g = datum[datadr2];
			g = 1/g;
			g = 100 * ((float) pow(f, g) - 1);
			datum[datadr1] = g;
			/* you overwrite the final amount, but keep the period untouched */
			f = 0.0f;
			g = 0.0f;

		  } else if (opr == 20) {
		  /* SWAP -- Swap two numbers. While trivial, it is annoying to novices. */
		  /* Mentally, you have to consider a third number or an idiotic trick, */
		  /* none of which is beneficial to "not getting interrupted" and focusing. */
		  /* So there is now an extra instruction for that. */

			f = datum[datadr2];
			datum[datadr2] = datum[datadr1];
			datum[datadr1] = f;

		  } else if (opr == 21) {
		  /* FACT -- Factorial of the SECOND number, i.e. of datum[datadr2]. */
		  /* The factorial of the second number ends up in the FIRST location. */
		  /* This is not a "self-modifying-data" command, because the factorial */
		  /* easily gets out of range, in which case the program gives zero. */
		  /* Force numbers to be positive. */

			j = (int) absvalu((int)(datum[datadr2]));
			if (j < 2) {
			  datum[datadr1] = 1.0f;
			} else if (j > 34) {
			  datum[datadr1] = 0.0f; /* signalling you messed it up */
			} else {
			  f = 1.0f;
			  for (i = 1; i <= j; i++) {
				f = f * i;
			  }
			  datum[datadr1] = f;
			}

		  /* ---- FROM NOW ON FOLLOW COMMANDS FOR NUMERIC RANGE MANIPULATION ------ */
		  /* Many of these actually have single-number application facilities, too. */

		  } else if (opr == 22) {
		  /* COPY -- COPY ADDRESS RANGE: */
		  /* Copy INTO the range between datadr1 and datadr2, both inclusive, */
		  /* the other range whose beginning and ending addresses are indicated */
		  /* upon call within the address datadr1 and the address datadr2. */
		  /* Clearly, this overwrites, too, the addresses originally stored in */
		  /* datadr1 and datadr2 at the call of the instruction. */
		  /* If the ranges are of unequal length, copy revolvingly or partially. */
		  /* Normally, first the low address, then the high addresses should be */
		  /* given - otherwise, assume a reversal of the range is desired. */

		  /* val is the source, adr is the destination - and will be OVERWRITTEN. */
		  /* Hence parameters can be placed under the destination-range */

		  /* adr1(val1) (low) adr2(val2) (high) is the "default" */

			/* Determine the SOURCE range h & k that shall be copied into */
			/* datadr1 and datadr2. (You CAN operate here with h & k, because IADR */
			/* would have taken effect ALREADY, should it have been issued. */
			h = (int) absvalu((int)(datum[datadr1]));
			k = (int) absvalu((int)(datum[datadr2]));

			/* Keep them within the allowed range. */
			if (h >= DATAMEM) {
			  h = DATAMEM - 1;
			}
			if (k >= DATAMEM) {
			  k = DATAMEM - 1;
			}

			j = h;
			if (datadr2 == datadr1) {
			  /* Then only one number is to be copied. */
			  datum[datadr1] = datum[h];
			  /* i.e. datum[datadr1] = datum[datum[datadr1]] */
			} else if (h == k) { /* then fill: */
			  /* You can use this to initialise a range to a value. */
			  f = datum[h];
			  if (datadr2 > datadr1) {
				for (i = datadr1; i <= datadr2; i++) {
				  datum[i] = f;
				}
			  } else {
				for (i = datadr2; i <= datadr1; i++) {
				  datum[i] = f;
				}
			  }
			} else if ((datadr2 > datadr1) && (k > h)) {
			  /* The "normal" case - make it revolving instead of "cutting" it. */
			  for (i = datadr1; i <= datadr2; i++) {
				datum[i] = datum[j];
				j++;
				if (j > k) {
				  j = h;
				}
			  }
			} else if ((datadr2 < datadr1) && (k < h))  {
			  /* The countdown case - here, the revolutions may fill differently. */
			  for (i = datadr1; i >= datadr2; i--) {
				datum[i] = datum[j];
				j--;
				if (j < k) {
				  j = h;
				}
			  }
			} else if ((datadr2 < datadr1) && (k > h)) {
			  /* this and the next will "reverse" a range of equal size */
			  /* they really only differ in how they would handle revolutions */
			  for (i = datadr1; i >= datadr2; i--) {
				datum[i] = datum[j];
				j++;
				if (j > k) {
				  j = h;
				}
			  }
			} else if ((datadr2 > datadr1) && (k < h)) {
			  for (i = datadr1; i <= datadr2; i++) {
				datum[i] = datum[j];
				j--;
				if (j < k) {
				  j = h;
				}
			  }

			}
			h = 0;
			k = 0;

		  } else if (opr == 23) {
		  /* FRIS -- FILL RANGE FROM INITIAL-VALUE BY STEP */
		  /* This is like looping a[n + 1] = a[n] + x.*/
		  /* datadr1 and datadr2 describe the range to be filled, */
		  /* datadr1 holds the initial value and datadr2 holds the step. */

			if ((datadr1 == datadr2) || (datadr2 == 0)) {
			  /* Do nothing - datadr1 has the init value, done!  */
			} else if (datadr1 < datadr2) {
			  f = datum[datadr2];
			  for (i = datadr1 + 1; i <= datadr2; i++) {
			  /* datadr1 + 1 as datadr1 already contains the initial value */
				datum[i] = datum[i - 1] + f;
			  }
			} else if (datadr1 > datadr2) { /* then do a count-down */
			  f = datum[datadr2];
			  for (i = datadr1 - 1; i >= datadr2; i--) {
				datum[i] = datum[i + 1] - f;
			  }
			}
			/* Giving a step-value of zero, this of course will just copy */
			/* the initial value onto an entire range. An initial value of zero */
			/* and a step of zero can be thus used to erase a memory section. */

		  } else if (opr == 24) {
		  /* MNMX -- "Minimax", determine the minimum and the maximum value in a */
		  /* range. Again adr1(adrA) adr2(adrB), where adr1 & adr2 contain */
		  /* the addresses of the range, signified by adrA and adrB, and afterwards */
		  /* adr1 and adr2 contain minimum and maximum numbers of said range. */
		  /* This variant takes sign into account - make the range positive if you */
		  /* actually care about the largest and smallest absolute values. */

			h = (int) absvalu((int)(datum[datadr1]));
			k = (int) absvalu((int)(datum[datadr2]));
			f = datum[h];
			g = datum[h]; /* Not a mistake - you start with the SAME number. */
			if (h < k) {
			  for (i = h + 1; i <= k; i++) { /* after the first number */
				if (f < datum[i]) {
				  f = datum[i];
				}
				if (g > datum[i]) {
				  g = datum[i];
				}
			  }
			} else if (h > k) {
			  for (i = k; i < h; i++) { /* before the last number */
				if (f < datum[i]) {
				  f = datum[i];
				}
				if (g > datum[i]) {
				  g = datum[i];
				}
			  }
			}
			datum[datadr1] = f; /* maximum */
			datum[datadr2] = g; /* minimum */

			h = 0;
			k = 0;

		  } else if (opr == 25) {
		  /* SORT -- SORT RANGE */
		  /* This is pretty naive and sacrifices run time to save memory. */
		  /* Basically, each time when numbers are "unsorted", swap them, */
		  /* and run through the whole array until no more swaps are done. */
		  /* If nothing is left to swap - then necessarily all must be in order. */
		  /* I am retaining this function despite the more interesting "coupled */
		  /* sort" below, because it is simply quite easy to use. */

			if (datadr1 == datadr2) {
			/* Do nothing at all. */
			} else if (datadr2 == (datadr1 + 1)) {
			  /* If only two numbers are given - either swap once, or not at all. */
			  if (datum[datadr1] > datum[datadr2]) {
				/* The next three operations are the swap. */
				f = datum[datadr2];
				datum[datadr2] = datum[datadr1];
				datum[datadr1] = f;
			  }
			} else if (datadr1 == (datadr2 + 1)) {
			  if (datum[datadr1] < datum[datadr2]) {
				f = datum[datadr2];
				datum[datadr2] = datum[datadr1];
				datum[datadr1] = f;
			  }
			} else if (datadr2 > datadr1) {
			  k = 1; /* flag: Do we have to sort? k = 1 means "yes". */
			  while (k == 1) {
				k = 0; /* Try to say you are done, and see is this contradicted. */
				for (i = datadr1; i < datadr2; i++) {
				  if (datum[i] > datum[i + 1]) {
					/* Then swap, so the lower address contains the lower value. */
					f = datum[i + 1];
					datum[i + 1] = datum[i];
					datum[i] = f;
					k = 1; /* Still work to do - contradicting k = 0 above. */
				  }
				}
			  }
			} else if (datadr2 < datadr1) {
			  k = 1; /* flag: Do we have to sort? */
			  while (k == 1) {
				k = 0; /* Try to say you are done. */
				for (i = datadr2; i < datadr1; i++) {
				  if (datum[i] < datum[i + 1]) {
					f = datum[i + 1];
					datum[i + 1] = datum[i];
					datum[i] = f;
					k = 1; /* Still work to do. */
				  }
				}
			  }
			}

		  } else if (opr == 26) {
		  /* CORS - COUPLED RANGE SORTING: The first range, starting from */
		  /* datadr1 + 1 and continuing to datum[datadr1], both inclusive, */
		  /* determines how datadr2 + 1 till datum[datadr2] is sorted. */
		  /* datadr1 is used as the "sorting index" for datadr2. */
		  /* This is very much like the general sorting routine. */
		  /* What this is for: to sort something "according to" something else. */
		  /* This should help solve "best"-value-tasks. */
		  /* If datadr2 is determined to be 0, then CORS really works like SORT, */
		  /* sorting the first range datum[datadr1 + 1] ... datum[datum[datadr1]]. */

			h = (int) absvalu((int)(datum[datadr1])); /* Only datum[datadr1] deterines range. */
			/* k is used for flagging only, and not for range determination, and */
			/* instead it is assumed that the range in datadr2 is of the same */
			/* nature as the range signified by datadr1; in other words, */
			/* datum[datadr2] is simply ignored. This is not a disadvantage - this */
			/* way, a range can be secondarily sorted that begins at datum[1]. */

			if ((datadr1 == h) || (datadr1 == h + 1) || (datadr1 == h - 1)) {
			/* DO NOTHING AT ALL 0 or 1 element "range" is not to be "sorted". */
			} else if (datadr1 == h - 2) {

			  if (datum[datadr1 + 1] > datum[datadr1 + 2]) {
				f = datum[datadr1 + 2];
				datum[datadr1 + 2] = datum[datadr1 + 1];
				datum[datadr1 + 1] = f;
				/* Do the same thing in the other range */
				if (datadr2 != 0) {
				  f = datum[datadr2 + 2];
				  datum[datadr2 + 2] = datum[datadr2 + 1];
				  datum[datadr2 + 1] = f;
				}
			  }

			} else if (datadr1 == h + 2) {

			  if (datum[datadr1 - 2] > datum[datadr1 - 1]) {
				f = datum[datadr1 - 2];
				datum[datadr1 - 2] = datum[datadr1 - 1];
				datum[datadr1 - 1] = f;
				/* Do the same thing in the other range */
				if (datadr2 != 0) {
				  f = datum[datadr2 + 2];
				  datum[datadr2 + 2] = datum[datadr2 + 1];
				  datum[datadr2 + 1] = f;
				}
			  }

			} else if (datadr1 + 1 < h) {
			  k = 1; /* flag: do we have to sort */
			  while (k == 1) {
				k = 0; /* try to say you are done */
				for (i = datadr1 + 1; i < h; i++) {
				  if (datum[i] > datum[i + 1]) {
					f = datum[i + 1];
					datum[i + 1] = datum[i];
					datum[i] = f;
					k = 1; /* still work to do */
					if (datadr2 != 0) {
					  j = i - datadr1; /* determine how the offset from datadr1 ... */
					  j = j + datadr2; /* ... and apply it to datadr2 ... */
					 /* ... using it to do the same thing in the other range */
					  f = datum[j + 1];
					  datum[j + 1] = datum[j];
					  datum[j] = f;
					}
				  }
				}
			  }
			} else if (datadr1 + 1 > h) {
			  k = 1; /* flag: do we have to sort */
			  while (k == 1) {
				k = 0; /* try to say you are done */
				for (i = h + 1; i < datadr1; i++) {
				  if (datum[i] < datum[i + 1]) {
					f = datum[i + 1];
					datum[i + 1] = datum[i];
					datum[i] = f;
					k = 1; /* still work to do */
					if (datadr2 != 0) {
					  j = i - datadr1; /* determine how the offset from datadr1 ... */
					  j = j + datadr2; /* ... and apply it to datadr2 ... */
					 /* ... using it to do the same thing in the other range */
					  f = datum[j + 1];
					  datum[j + 1] = datum[j];
					  datum[j] = f;
					}
				  }
				}
			  }
			}

			h = 0;
			k = 0;

		  } else if (opr == 27) {
		  /* TURN -- TURN A RANGE UPSIDE DOWN. */
		  /* If in the range the numbers were 1.0 3.0 -3.8 2.1, make them be */
		  /* instead 2.1 -3.8 3.0 1.0. The range is assumed between datadr1 and */
		  /* datadr2, both inclusive. */

			if ((datadr1 == datadr2) || (datadr2 == 0)) {
				/* do nothing */
			} else if (datadr1 < datadr2) {
			  for (i = datadr1; i <= (datadr1 + ((datadr2 - datadr1) / 2)); i++) {
				f = datum[i];
				datum[i] = datum[datadr2 - (i - datadr1)];
				datum[datadr2 - (i - datadr1)] = f;
			  }
			} else if (datadr1 > datadr2) {
			  for (i = datadr2; i <= (datadr2 + ((datadr1 - datadr2) / 2)); i++) {
				f = datum[i];
				datum[i] = datum[datadr1 - (i - datadr2)];
				datum[datadr1 - (i - datadr2)] = f;
			  }
			}

		  } else if (opr == 28) {
		  /* SUMR -- SUM OF ADDRESS RANGE - RESULT ALWAYS IN FIRST ADDRESS. */
		  /* Sum up the range signified by datum[datum[datadr1]] and */
		  /* datum[datum[datadr2]], and place the result into datum[datadr1]. */
			h = (int) absvalu((int)(datum[datadr1]));
			k = (int) absvalu((int)(datum[datadr2]));
			if ((h == k) || (k == 0)) { /* nothing to sum up if only one number */
				datum[datadr1] = datum[h];
			} else if (h < k) {
			  datum[datadr1] = 0.0f;
			  for (i = h; i <= k; i++) {
				datum[datadr1] = datum[datadr1] + datum[i];
			  }
			} else if (h > k) {
			  datum[datadr1] = 0.0f;
			  for (i = k; i <= h; i++) {
				datum[datadr1] = datum[datadr1] + datum[i];
			  }
			}
			h = 0;
			k = 0;

		  } else if (opr == 29) {
		  /* SUSQ -- SUM OF SQUARES AND SQUARE ROOTED SUM OF SQUARES */
		  /* For vectors, statistics,... The range ADDRESSES are held in */
		  /* datadr1 and datadr2, i.e. this is again indirect, and after execution, */
		  /* datadr1 has the sum of squares and datadr2 has the square root of it. */
		  /* Only anything to do if the addresses really are in a RANGE: */
			h = (int) absvalu((int)(datum[datadr1]));
			k = (int) absvalu((int)(datum[datadr2]));
			if ((h == k) || (k == 0)) {
				datum[datadr1] = absvalu(datum[h]); /* (float) sqrt(x^2) = absvalu(x) */
			} else if (h < k) {
			  datum[datadr1] = 0.0f;
			  for (i = h; i <= k; i++) {
				datum[datadr1] = datum[datadr1] + (datum[i] * datum[i]);
			  }
			} else if (h > k) {
			  datum[datadr1] = 0.0f;
			  for (i = k; i <= h; i++) {
				datum[datadr1] = datum[datadr1] + (datum[i] * datum[i]);
			  }
			}
			  datum[datadr2] = (float) sqrt(datum[datadr1]);
			h = 0;
			k = 0;

		  } else if (opr == 30) {
		  /* The next commands focus on a SINGLE range - and often have */
		  /* meaning even if used on a single number, by setting datadr2 to */
		  /* the same value as datadr1 or to zero (which is shorthand for that). */
		  /* This is in contrast to actual range-range-commands, which */
		  /* operate on TWO ranges, not on ONE range. */

		  /* IXTH -- turn each X into 1/Xth in a range. X=0 gives 0, as always.  */
		  /* The range is signified by datadr1 and datadr2, and if these are equal, */
		  /* or if datadr2 is zero, then just perform that operation on the first */
		  /* number, i.e. on datum[datadr1]. */
			if ((datadr1 == datadr2) || (datadr2 == 0)) {
				if (datum[datadr1] != 0.0f) {
				  datum[datadr1] = 1.0f/datum[datadr1];
				} else {
				  datum[datadr1] = 0.0f;
				}
			} else if (datadr1 < datadr2) {
			  for (i = datadr1; i <= datadr2; i++) {
				if (datum[i] != 0.0f) {
				  datum[i] = 1.0f/datum[i];
				} else {
				  datum[i] = 0.0f;
				}
			  }
			} else if (datadr1 > datadr2) {
			  for (i = datadr2; i <= datadr1; i++) {
				if (datum[i] != 0.0f) {
				  datum[i] = 1.0f/datum[i];
				} else {
				  datum[i] = 0.0f;
				}
			  }
			}

		  } else if (opr == 31) {
		  /* ABSR -- ABSOLUTE VALUE OF RANGE. */
		  /* The range is signified by datadr1 and datadr2, and if these are equal, */
		  /* or if datadr2 is zero, then just perform that operation on the first */
		  /* number, i.e. on datum[datadr1]. Same principle as above. */

			if ((datadr1 == datadr2) || (datadr2 == 0)) {
				datum[datadr1] = absvalu(datum[datadr1]);
			} else if (datadr1 < datadr2) {
			  for (i = datadr1; i <= datadr2; i++) {
				datum[i] = absvalu(datum[i]);
			  }
			} else if (datadr1 > datadr2) {
			  for (i = datadr2; i <= datadr1; i++) {
				datum[i] = absvalu(datum[i]);
			  }
			}

		  } else if (opr == 32) {
		  /* SQRT -- SQUARE ROOT ABSOLUTES IN RANGE */
		  /* The range is signified by datadr1 and datadr2, and if these are equal, */
		  /* or if datadr2 is zero, then just perform that operation on the first */
		  /* number, i.e. on datum[datadr1]. Same principle as above. */

			if ((datadr1 == datadr2) || (datadr2 == 0)) {
				datum[datadr1] = (float) sqrt(absvalu(datum[datadr1]));
			} else if (datadr1 < datadr2) {
			  for (i = datadr1; i <= datadr2; i++) {
				datum[i] = (float) sqrt(absvalu(datum[i]));
			  }
			} else if (datadr1 > datadr2) {
			  for (i = datadr2; i <= datadr1; i++) {
				datum[i] = (float) sqrt(absvalu(datum[i]));
			  }
			}

		  } else if (opr == 33) {
		  /* SQUA -- SQUARE RANGE */
		  /* The range is signified by datadr1 and datadr2, and if these are equal, */
		  /* or if datadr2 is zero, then just perform that operation on the first */
		  /* number, i.e. on datum[datadr1]. Same principle as above. */

			if ((datadr1 == datadr2) || (datadr2 == 0)) {
				datum[datadr1] = datum[datadr1] * datum[datadr1];
			} else if (datadr1 < datadr2) {
			  for (i = datadr1; i <= datadr2; i++) {
				datum[i] = datum[i] * datum[i];
			  }
			} else if (datadr1 > datadr2) {
			  for (i = datadr2; i <= datadr1; i++) {
				datum[i] = datum[i] * datum[i];
			  }
			}

		  } else if (opr == 34) {
		  /* CBRT -- CUBE ROOT IN RANGE */
		  /* The range is signified by datadr1 and datadr2, and if these are equal, */
		  /* or if datadr2 is zero, then just perform that operation on the first */
		  /* number, i.e. on datum[datadr1]. Same principle as above. */

			if ((datadr1 == datadr2) || (datadr2 == 0)) {
				datum[datadr1] = (float) cbrt(datum[datadr1]);
			} else if (datadr1 < datadr2) {
			  for (i = datadr1; i <= datadr2; i++) {
				datum[i] = (float) cbrt(datum[i]);
			  }
			} else if (datadr1 > datadr2) {
			  for (i = datadr2; i <= datadr1; i++) {
				datum[i] = (float) cbrt(datum[i]);
			  }
			}

		  } else if (opr == 35) {
		  /* CUBE RANGE */
		  /* The range is signified by datadr1 and datadr2, and if these are equal, */
		  /* or if datadr2 is zero, then just perform that operation on the first */
		  /* number, i.e. on datum[datadr1]. Same principle as above. */

			if ((datadr1 == datadr2) || (datadr2 == 0)) {
				datum[datadr1] = datum[datadr1] * datum[datadr1] * datum[datadr1];
			} else if (datadr1 < datadr2) {
			  for (i = datadr1; i <= datadr2; i++) {
				datum[i] = datum[i] * datum[i] * datum[i];
			  }
			} else if (datadr1 > datadr2) {
			  for (i = datadr2; i <= datadr1; i++) {
				datum[i] = datum[i] * datum[i] * datum[i];
			  }
			}

		  } else if (opr == 36) {
		  /* LNRN -- NATURAL LOGARITHM OF RANGE. */
		  /* The numbers are forced into being positive, and a logarithm of zero */
		  /* gives zero. */

		  /* You can obtain any logarithm or power you like using formulae such as */
		  /* A ^ B =  X ^ (B * logX (A)) as well as logA (B) = logX (B) / logX (A). */
		  /* Obviously, that ominous "X" can also be "e". */

		  /* The range is signified by datadr1 and datadr2, and if these are equal, */
		  /* or if datadr2 is zero, then just perform that operation on the first */
		  /* number, i.e. on datum[datadr1]. Same principle as above. */

            /* OMITTED - DYSFUNCTIONAL
			if ((datadr1 == datadr2) || (datadr2 == 0)) {
				if (datum[datadr1] != 0.0f) {
				  datum[datadr1] = (float) log(absvalu(datum[datadr1]));
				} else {
				  datum[datadr1] = 0.0f;
				}
			} else if (datadr1 < datadr2) {
			  for (i = datadr1; i <= datadr2; i++) {
				if (datum[i] != 0.0f) {
				  datum[i] = (float) log(absvalu(datum[i]));
				} else {
				  datum[i] = 0.0f;
				}
			  }
			} else if (datadr1 > datadr2) {
			  for (i = datadr2; i <= datadr1; i++) {
				if (datum[i] != 0.0f) {
				  datum[i] = (float) log(absvalu(datum[i]));
				} else {
				  datum[i] = 0.0f;
				}
			  }
			}
			*/

		  } else if (opr == 37) {
		  /* EXPR -- e^X of range. */

		  /* You can obtain any logarithm or power you like using formulae such as */
		  /* A ^ B =  X ^ (B * logX (A)) as well as logA (B) = logX (B) / logX (A). */
		  /* Obviously, that ominous "X" can also be "e". See above. */

		  /* The range is signified by datadr1 and datadr2, and if these are equal, */
		  /* or if datadr2 is zero, then just perform that operation on the first */
		  /* number, i.e. on datum[datadr1]. Same principle as above. */

			if ((datadr1 == datadr2) || (datadr2 == 0)) {
				datum[datadr1] = (float) exp(datum[datadr1]);
			} else if (datadr1 < datadr2) {
			  for (i = datadr1; i <= datadr2; i++) {
				datum[i] = (float) exp(datum[i]);
			  }
			} else if (datadr1 > datadr2) {
			  for (i = datadr2; i <= datadr1; i++) {
				datum[i] = (float) exp(datum[i]);
			  }
			}

		  } else if (opr == 38) {
		  /* Now follow a few trigonometric functions. It my experience, */
		  /* in the vast majority of cases when you are dealing with */
		  /* trigonometric functions, practically you are dealing with degrees, */
		  /* not radian. Hence, these functions are all oriented to degrees. */
		  /* If, for any reason, a conversion between radian and degrees becomes */
		  /* necessary, this function and the next one will take care of it. */

		  /* RADE -- CONVERT RANGE FROM RADIANS TO DEGREES */
		  /* degrees = radians * 180 / pi ... radians = degrees * pi / 180 */

		  /* The range is signified by datadr1 and datadr2, and if these are equal, */
		  /* or if datadr2 is zero, then just perform that operation on the first */
		  /* number, i.e. on datum[datadr1]. Same principle as above. */

			if ((datadr1 == datadr2) || (datadr2 == 0)) {
				datum[datadr1] = datum[datadr1] * 180 / PI;
			} else if (datadr1 < datadr2) {
			  for (i = datadr1; i <= datadr2; i++) {
				datum[i] = datum[i] * 180 / PI;
			  }
			} else if (datadr1 > datadr2) {
			  for (i = datadr2; i <= datadr1; i++) {
				datum[i] = datum[i] * 180 / PI;
			  }
			}

		  } else if (opr == 39) {
		  /* DERA -- CONVERT RANGE FROM DEGREES TO RADIANS */
		  /* degrees = radians * 180 / pi ... radians = degrees * pi / 180 */

		  /* The range is signified by datadr1 and datadr2, and if these are equal, */
		  /* or if datadr2 is zero, then just perform that operation on the first */
		  /* number, i.e. on datum[datadr1]. Same principle as above. */

			if ((datadr1 == datadr2) || (datadr2 == 0)) {
				datum[datadr1] = datum[datadr1] * PI / 180;
			} else if (datadr1 < datadr2) {
			  for (i = datadr1; i <= datadr2; i++) {
				datum[i] = datum[i] * PI / 180;
			  }
			} else if (datadr1 > datadr2) {
			  for (i = datadr2; i <= datadr1; i++) {
				datum[i] = datum[i] * PI / 180;
			  }
			}

		  } else if (opr == 40) {
		  /* SIND -- Sinus of degrees in a range. */

		  /* The range is signified by datadr1 and datadr2, and if these are equal, */
		  /* or if datadr2 is zero, then just perform that operation on the first */
		  /* number, i.e. on datum[datadr1]. Same principle as above. */

			if ((datadr1 == datadr2) || (datadr2 == 0)) {
				datum[datadr1] = datum[datadr1] * PI / 180;
				datum[datadr1] = (float) java.lang.Math.sin(datum[datadr1]);
			} else if (datadr1 < datadr2) {
			  for (i = datadr1; i <= datadr2; i++) {
				datum[i] = datum[i] * PI / 180;
				datum[i] = (float) java.lang.Math.sin(datum[i]);
			  }
			} else if (datadr1 > datadr2) {
			  for (i = datadr2; i <= datadr1; i++) {
				datum[i] = datum[i] * PI / 180;
				datum[i] = (float) java.lang.Math.sin(datum[i]);
			  }
			}

		  } else if (opr == 41) {
		  /* COSD -- Cosinus of degrees in a range.  */

		  /* The range is signified by datadr1 and datadr2, and if these are equal, */
		  /* or if datadr2 is zero, then just perform that operation on the first */
		  /* number, i.e. on datum[datadr1]. Same principle as above. */

			if ((datadr1 == datadr2) || (datadr2 == 0)) {
				datum[datadr1] = datum[datadr1] * PI / 180;
				datum[datadr1] = (float) java.lang.Math.cos(datum[datadr1]);
			} else if (datadr1 < datadr2) {
			  for (i = datadr1; i <= datadr2; i++) {
				datum[i] = datum[i] * PI / 180;
				datum[i] = (float) java.lang.Math.cos(datum[i]);
			  }
			} else if (datadr1 > datadr2) {
			  for (i = datadr2; i <= datadr1; i++) {
				datum[i] = datum[i] * PI / 180;
				datum[i] = (float) java.lang.Math.cos(datum[i]);
			  }
			}

		  } else if (opr == 42) {
		  /* TAND -- Tangens of degrees in a range.  */
		  /* The cotangens you find simply by turning this to 1/X. */

		  /* The range is signified by datadr1 and datadr2, and if these are equal, */
		  /* or if datadr2 is zero, then just perform that operation on the first */
		  /* number, i.e. on datum[datadr1]. Same principle as above. */

			if ((datadr1 == datadr2) || (datadr2 == 0)) {
			  datum[datadr1] = datum[datadr1] * PI / 180;
			  if ((float) java.lang.Math.cos(datum[datadr1]) != 0.0f) {
				datum[datadr1] = (float) java.lang.Math.sin(datum[datadr1])/(float) java.lang.Math.cos(datum[datadr1]);
			  } else {
				datum[datadr1] = 0.0f;
			  }
			} else if (datadr1 < datadr2) {
			  for (i = datadr1; i <= datadr2; i++) {
				if ((float) java.lang.Math.cos(datum[i]) != 0.0f) {
				  datum[i] = datum[i] * PI / 180;
				  datum[i] = (float) java.lang.Math.sin(datum[i])/(float) java.lang.Math.cos(datum[i]);
				} else {
				  datum[i] = 0.0f;
				}
			  }
			} else if (datadr1 > datadr2) {
			  for (i = datadr2; i <= datadr1; i++) {
				if ((float) java.lang.Math.cos(datum[i]) != 0.0f) {
				  datum[i] = datum[i] * PI / 180;
				  datum[i] = (float) java.lang.Math.sin(datum[i])/(float) java.lang.Math.cos(datum[i]);
				} else {
				  datum[i] = 0.0f;
				}
			  }
			}

		  } else if (opr == 43) {
		  /* ASND -- Arcussinus of a range, giving degrees.  */

		  /* The range is signified by datadr1 and datadr2, and if these are equal, */
		  /* or if datadr2 is zero, then just perform that operation on the first */
		  /* number, i.e. on datum[datadr1]. Same principle as above. */
		  
			/* OMITTED */

		  } else if (opr == 44) {
		  /* ACSD -- Arcuscosinus of a range, giving degrees. */

		  /* The range is signified by datadr1 and datadr2, and if these are equal, */
		  /* or if datadr2 is zero, then just perform that operation on the first */
		  /* number, i.e. on datum[datadr1]. Same principle as above. */

			/* OMITTED */

		  } else if (opr == 45) {
		  /* ATND -- Arcustangens of a range, giving degrees. */

		  /* The range is signified by datadr1 and datadr2, and if these are equal, */
		  /* or if datadr2 is zero, then just perform that operation on the first */
		  /* number, i.e. on datum[datadr1]. Same principle as above. */

			/* OMITTED */

		  } else if (opr == 46) {
		  /* MSTD -- MEAN AND STANDARD DEVIATION ON A SAMPLE. */
		  /* I.e. this is WITH "Bessel's correction" for the standard deviation. */
		  /* A statistical function. datadr1 and datadr2 indicate a range, */
		  /* datum[datum[datadr1]] till datum[datum[datadr2]]. After application, */
		  /* the mean of the range is contained in datadr1 and the standard */
		  /* deviation in datadr2. A Gaussian distribution is assumed. */

		    h = (int) absvalu((int)(datum[datadr1]));
		    k = (int) absvalu((int)(datum[datadr2]));
                   j = (int) absvalu(h - k); /* how many cells minus 1*/
               
                   if ((h == k) || (k == 0)) { /* "very funny" - no range */
                     datum[datadr1] = datum[h];
                   } else if (h < k) {
                     datum[datadr1] = 0.0f;
                     for (i = h; i <= k; i++) {
                       datum[datadr1] = datum[datadr1] + datum[i];
                     }
                   } else if (h > k) {
                     datum[datadr1] = 0.0f;
                     for (i = k; i <= h; i++) {
                       datum[datadr1] = datum[datadr1] + datum[i];
                     }
                   }
               
                   datum[datadr1] = datum[datadr1]/(j + 1);
               
                   if (datadr2 != datadr1) {
                     if ((h == k) || (k == 0)) { /* "very funny" - no range */
                       datum[datadr2] = 0;
                       /* No standard deviation can be given. */
                     } else if (h < k) {
                       datum[datadr2] = 0.0f;
                       for (i = h; i <= k; i++) {
                         datum[datadr2] = datum[datadr2]
                           + ((datum[i] - datum[datadr1]) * (datum[i] - datum[datadr1]));
                       }
                     } else if (h > k) {
                       datum[datadr2] = 0.0f;
                       for (i = k; i <= h; i++) {
                         datum[datadr2] = datum[datadr2]
                           + ((datum[i] - datum[datadr1]) * (datum[i] - datum[datadr1]));
                       }
                     }
               
                     datum[datadr2] = (float) java.lang.Math.sqrt(datum[datadr2]/j);
                   }
                   h = 0;
                   k = 0;

		  } else if (opr == 47) {
		  /* ZERO -- ZERO OUT A DATA RANGE. */
		  /* While there are numerous ways to do it, this will be the most obvious */
		  /* choice for the user. - As there is no provision for changing */
		  /* instructions in a running program, there is no corresponding memory */
		  /* clearance for instructions; this must be done manually at the REPL. */
		  /* NOTE THE SHORTHAND OF 0 0 TO CLEAR ALL MEMORY. This may be useful in */
		  /* order to clear residual values or begin a calculation anew, e.g. */
		  /* asking the user for input. */

			if ((datadr1 == 0) && (datadr2 == 0)) {
			/* If BOTH numbers are zero, not just datadr2 but also datadr1, */
			/* then see this as shorthand for "CLEAR ALL MEMORY". */
			  for (i = 1 ; i < DATAMEM ; i++) {
				datum[i] = 0.0f;
			  }
			} else if ((datadr1 == datadr2) || (datadr2 == 0)) {
			/* If JUST datadr2 is 0, see it as shorthand to just clear datadr1. */
				datum[datadr1] = 0.0f;
			} else if (datadr1 < datadr2) {
			  for (i = datadr1; i <= datadr2; i++) { 
				datum[i] = 0.0f;
			  }
			} else if (datadr1 > datadr2) {
			  for (i = datadr2; i <= datadr1; i++) { 
				datum[i] = 0.0f;
			  }
			}

		  } else if (opr == 48) {
		  /* RAND -- GENERATE RANDOM NUMBERS. */
		  /* Whereby datadr1 contains the minimum allowed number and datadr2 */
		  /* contains the maximum allowed number of the range. The numbers are then */
		  /* generated within that range, eventually also overwriting the minimum */
		  /* and the maximum values with random numbers within the desired limits. */

			/* OMITTED */

		  } else if (opr == 49) {
		  /* RUND -- (int) RANGE. */

		  /* This and the following two functions attempt to turn floats into */
		  /* integers. Again, they can be applied to a single number or a range. */

            /* OMITTED IN PART - JUST GIVES THE INTEGER PART*/

			i = datadr1;
			if ((datadr1 == datadr2) || (datadr2 == 0)) {
				datum[i] = (int)(datum[i]);
			} else if (datadr1 < datadr2) {
			  for (i = datadr1; i <= datadr2; i++) {
				datum[i] = (int)(datum[i]);
			  }
			} else if (datadr1 > datadr2) {
			  for (i = datadr2; i <= datadr1; i++) {
				datum[i] = (int)(datum[i]);
			  }
			}

		  } else if (opr == 50) {
		  /* CEIL -- CEILING RANGE */

			i = datadr1;
			if ((datadr1 == datadr2) || (datadr2 == 0)) {
				datum[i] = (float) java.lang.Math.ceil(datum[i]);
			} else if (datadr1 < datadr2) {
			  for (i = datadr1; i <= datadr2; i++) {
				datum[i] = (float) java.lang.Math.ceil(datum[i]);
			  }
			} else if (datadr1 > datadr2) {
			  for (i = datadr2; i <= datadr1; i++) {
				datum[i] = (float) java.lang.Math.ceil(datum[i]);
			  }
			}

		  } else if (opr == 51) {
		  /* TANH -- TANH OF RANGE */
		  /* (float) java.lang.Math.tanh(x) = (e^x - e^(-x)) / (e^x + e^(-x)) */
		  /* Just in case anybody does anything logistic or wants a neural network */
		  /* to be programmed dynamically - this is a famous activation function. */

		  /* OMITTED */

		  } else if (opr == 52) {
		  /* DTNH -- DERIVATIVE OF TANH */
		  /* deriv(float) java.lang.Math.tanh(x) = 1 - (float) java.lang.Math.tanh(x) * (float) java.lang.Math.tanh(x) */
		  /* This is usually used in conjunction with the above. */

		  /* OMITTED */

		  /* ---- FROM NOW ON FOLLOW RANGE-WITH-RANGE-OPERATIONS ------------------ */
		  } else if (opr == 53) {
		  /* RANGE-RANGE-COMMANDS (AND RANGE-NUMBER-COMMANDS): */

		  /* Essentially, with ranges, do something for each corresponding position */
		  /* in two ranges, and end when the shorter range has finished. - A number */
		  /* to range application means to modify each position in a range by said */
		  /* number in the manner specified by the respective operator. */

		  /* The first address will be one BEFORE or the one AFTER the range, and */
		  /* it will contain the pointer to the end (or beginning) of the */
		  /* respective range. That is: A[-1] --> An ; B[-1] --> Bn. */
		  /* The result will be in the first range. */
		  /* To keep things simple, if the second address is NOT */
		  /* higher or equal to the first address, simply DO NOTHING. */

		  /* I was totally unsure shouldn't I use datum[0] as "accumulator" here, */
		  /* but it would make tracing errors more difficult, as datum[0] would get */
		  /* all the time overwritten. But e.g. supplying the two addresses and */
		  /* have datum[0] supply the length would be totally feasible, */
		  /* as an alternative - just then avoid "nuking" it with 0 all the time. */
		  /* However, this would NOT make it easier for the targeted user base, */
		  /* and "ease of use by the user" is a design priority: without this */
		  /* permanent overwriting, the "accumulator" of each range may be printed */
		  /* to see actually where you may have messed up, not as with datum[0]. */
		  /* For the user, the advice is to leave at least one number BELOW and */
		  /* at least one number ABOVE each interesting range unused, so as to */
		  /* be able to use range commands (both top-down and bottom-up). */

		  /* datum[datadr1 + 1] is the range begin */
		  /* datum[h] is the range end; for k & datadr2, respectively */
		  /* So each material range actually begins NOT at datadr1 and datadr2, but */
		  /* at one address HIGHER than them. */

		  /* PLUR -- ADD TWO RANGES. */
		  /* If they are not of equal length, the operation will stop with the end */
		  /* of the shorter range. */

			h = (int) absvalu((int)(datum[datadr1]));
			k = (int) absvalu((int)(datum[datadr2]));
			if ((h == datadr1) || (k == datadr2)) {
			  /* then simply do nothing, there is exactly NO range to operate on. */
			} else if ((h > datadr1) && (k > datadr2)) {
			  j = datadr2 + 1;
			  for (i = datadr1 + 1; ((i <= h) && (j <= k)); i++) {
				datum[i] = datum[i] + datum[j];
				j++;
			  }
			} else if ((h < datadr1) && (k < datadr2)) {
			  /* still, datadr have the ADDRESSES and cannot be used themselves */
			  j = datadr2 - 1;
			  for (i = datadr1 - 1; ((i >= h) && (j >= k)); i--) {
				datum[i] = datum[i] + datum[j];
				j--;
			  }
			} else if ((h > datadr1) && (k < datadr2)) {
			  /* the mixed modes give you a "reversed array" operation */
			  j = datadr2 - 1;
			  for (i = datadr1 + 1; ((i <= h) && (j >= k)); i++) {
				datum[i] = datum[i] + datum[j];
				j--;
			  }
			} else if ((h < datadr1) && (k > datadr2)) {
			  /* the mixed modes give you a "reversed array" operation */
			  j = datadr2 + 1;
			  for (i = datadr1 - 1; ((i >= h) && (j <= k)); i--) {
				datum[i] = datum[i] + datum[j];
				j++;
			  }
			}

			h = 0;
			k = 0;

		  } else if (opr == 54) {
		  /* MINR -- SUBTRACT A RANGE FROM A RANGE */
		  /* If they are not of equal length, the operation will stop with the end */
		  /* of the shorter range. */

			h = (int) absvalu((int)(datum[datadr1]));
			k = (int) absvalu((int)(datum[datadr2]));
			if ((h == datadr1) || (k == datadr2)) {
			  /* then simply do nothing, there is exactly NO range to operate on. */
			} else if ((h > datadr1) && (k > datadr2)) {
			  j = datadr2 + 1;
			  for (i = datadr1 + 1; ((i <= h) && (j <= k)); i++) {
				datum[i] = datum[i] - datum[j];
				j++;
			  }
			} else if ((h < datadr1) && (k < datadr2)) {
			  /* still, datadr have the ADDRESSES and cannot be used themselves */
			  j = datadr2 - 1;
			  for (i = datadr1 - 1; ((i >= h) && (j >= k)); i--) {
				datum[i] = datum[i] - datum[j];
				j--;
			  }
			} else if ((h > datadr1) && (k < datadr2)) {
			  /* the mixed modes give you a "reversed array" operation */
			  j = datadr2 - 1;
			  for (i = datadr1 + 1; ((i <= h) && (j >= k)); i++) {
				datum[i] = datum[i] - datum[j];
				j--;
			  }
			} else if ((h < datadr1) && (k > datadr2)) {
			  /* the mixed modes give you a "reversed array" operation */
			  j = datadr2 + 1;
			  for (i = datadr1 - 1; ((i >= h) && (j <= k)); i--) {
				datum[i] = datum[i] - datum[j];
				j++;
			  }
			}

			h = 0;
			k = 0;

		  } else if (opr == 55) {
		  /* MULR -- MULTIPLY A RANGE WITH A RANGE */
		  /* If they are not of equal length, the operation will stop with the end */
		  /* of the shorter range. */

			h = (int) absvalu((int)(datum[datadr1]));
			k = (int) absvalu((int)(datum[datadr2]));
			if ((h == datadr1) || (k == datadr2)) {
			  /* then simply do nothing, there is exactly NO range to operate on. */
			} else if ((h > datadr1) && (k > datadr2)) {
			  j = datadr2 + 1;
			  for (i = datadr1 + 1; ((i <= h) && (j <= k)); i++) {
				datum[i] = datum[i] * datum[j];
				j++;
			  }
			} else if ((h < datadr1) && (k < datadr2)) {
			  /* still, datadr have the ADDRESSES and cannot be used themselves */
			  j = datadr2 - 1;
			  for (i = datadr1 - 1; ((i >= h) && (j >= k)); i--) {
				datum[i] = datum[i] * datum[j];
				j--;
			  }
			} else if ((h > datadr1) && (k < datadr2)) {
			  /* the mixed modes give you a "reversed array" operation */
			  j = datadr2 - 1;
			  for (i = datadr1 + 1; ((i <= h) && (j >= k)); i++) {
				datum[i] = datum[i] * datum[j];
				j--;
			  }
			} else if ((h < datadr1) && (k > datadr2)) {
			  /* the mixed modes give you a "reversed array" operation */
			  j = datadr2 + 1;
			  for (i = datadr1 - 1; ((i >= h) && (j <= k)); i--) {
				datum[i] = datum[i] * datum[j];
				j++;
			  }
			}

			h = 0;
			k = 0;

		  } else if (opr == 56) {
		  /* DIVR -- DIVIDE A RANGE BY A RANGE */
		  /* If they are not of equal length, the operation will stop with the end */
		  /* of the shorter range. A division by 0 nullifies the dividend, */
		  /* as before on single-number-operations. */

			h = (int) absvalu((int)(datum[datadr1]));
			k = (int) absvalu((int)(datum[datadr2]));
			if ((h == datadr1) || (k == datadr2)) {
			  /* then simply do nothing, there is exactly NO range to operate on. */
			} else if ((h > datadr1) && (k > datadr2)) {
			  j = datadr2 + 1;
			  for (i = datadr1 + 1; ((i <= h) && (j <= k)); i++) {
				if (datum[j] != 0.0f) {
				  datum[i] = datum[i] / datum[j];
				} else {
				  datum[i] = 0.0f;
				}
				j++;
			  }
			} else if ((h < datadr1) && (k < datadr2)) {
			  /* still, datadr have the ADDRESSES and cannot be used themselves */
			  j = datadr2 - 1;
			  for (i = datadr1 - 1; ((i >= h) && (j >= k)); i--) {
				if (datum[j] != 0.0f) {
				  datum[i] = datum[i] / datum[j];
				} else {
				  datum[i] = 0.0f;
				}
				j--;
			  }
			} else if ((h > datadr1) && (k < datadr2)) {
			  /* the mixed modes give you a "reversed array" operation */
			  j = datadr2 - 1;
			  for (i = datadr1 + 1; ((i <= h) && (j >= k)); i++) {
				if (datum[j] != 0.0f) {
				  datum[i] = datum[i] / datum[j];
				} else {
				  datum[i] = 0.0f;
				}
				j--;
			  }
			} else if ((h < datadr1) && (k > datadr2)) {
			  /* the mixed modes give you a "reversed array" operation */
			  j = datadr2 + 1;
			  for (i = datadr1 - 1; ((i >= h) && (j <= k)); i--) {
				if (datum[j] != 0.0f) {
				  datum[i] = datum[i] / datum[j];
				} else {
				  datum[i] = 0.0f;
				}
				j++;
			  }
			}

			h = 0;
			k = 0;

		  /* ---- FROM NOW ON FOLLOW RANGE-WITH-SINGLE-NUMBER-OPERATIONS ---------- */
		  } else if (opr == 57) {
		  /* PLUN -- ADD A NUMBER TO A RANGE. */

			h = (int) absvalu((int)(datum[datadr1]));
			if (h == datadr1) {
			  /* then simply do nothing, there is exactly NO range to operate on. */
			} else if (h > datadr1) {
			  for (i = datadr1 + 1; i <= h; i++) {
				datum[i] = datum[i] + datum[datadr2];
			  }
			} else if (h < datadr1) {
			  for (i = datadr1 - 1; i >= h; i--) {
				datum[i] = datum[i] + datum[datadr2];
			  }
			}

			h = 0;
			k = 0;

		  } else if (opr == 58) {
		  /* MINN -- SUBTRACT A NUMBER FROM A RANGE. */

			h = (int) absvalu((int)(datum[datadr1]));
			if (h == datadr1) {
			  /* then simply do nothing, there is exactly NO range to operate on. */
			} else if (h > datadr1) {
			  for (i = datadr1 + 1; i <= h; i++) {
				datum[i] = datum[i] - datum[datadr2];
			  }
			} else if (h < datadr1) {
			  for (i = datadr1 - 1; i >= h; i--) {
				datum[i] = datum[i] - datum[datadr2];
			  }
			}

			h = 0;
			k = 0;

		  } else if (opr == 59) {
		  /* MULN -- MULTIPLY A RANGE WITH A NUMBER. */

			h = (int) absvalu((int)(datum[datadr1]));
			if (h == datadr1) {
			  /* then simply do nothing, there is exactly NO range to operate on. */
			} else if (h > datadr1) {
			  for (i = datadr1 + 1; i <= h; i++) {
				datum[i] = datum[i] * datum[datadr2];
			  }
			} else if (h < datadr1) {
			  for (i = datadr1 - 1; i >= h; i--) {
				datum[i] = datum[i] * datum[datadr2];
			  }
			}

			h = 0;
			k = 0;


		  } else if (opr == 60) {
		  /* DIVN -- DIVIDE A RANGE BY A NUMBER. */
		  /* A division by 0 nullifies the dividend, */
		  /* as before on single-number-operations. */

			h = (int) absvalu((int)(datum[datadr1]));
			if (h == datadr1) {
			  /* then simply do nothing, there is exactly NO range to operate on. */
			} else if (h > datadr1) {
			  if (datum[datadr2] != 0.0f) {
				for (i = datadr1 + 1; i <= h; i++) {
				  datum[i] = datum[i] / datum[datadr2];
				}
			  } else {
				for (i = datadr1 + 1; i <= h; i++) {
				  datum[i] = 0.0f;
				}
			  }
			} else if (h < datadr1) {
			  if (datum[datadr2] != 0.0f) {
				for (i = datadr1 - 1; i >= h; i--) {
				  datum[i] = datum[i] / datum[datadr2];
				}
			  } else {
				for (i = datadr1 - 1; i >= h; i--) {
				  datum[i] = 0.0f;
				}
			  }
			}

			h = 0;
			k = 0;

		  /* ---- LAST COMMAND OF THE SIX-BIT-RANGE IS RESERVED FOR THE USER ------ */
		  } else if (opr == 63) {
			/* USER -- RESERVED, perhaps for GPIO */
		  }
		  /* ------- END DECODING ------- */




		/* Advance to the next instruction UNLESS it was an */
		/* immediate instruction - in such a case, simply return to the REPL. */
		if (opr != 1) {
			if (pc > 0) {
			  pc++;
			} else if (pc == 0) {
			/* do not increase pc=0: just execute immediately and return */
			/* you can even "mix" automatic and immediate mode and return or JUMP, */
			/* whereby a jump will change pc and then go on - your entry point is */
			/* thus freely selectable. A program can be set for automatic execution */
			/* perhaps by writing in instruction[0] a jump to the entry point, but */
			/* herein this is not (yet) implemented; the REPL seems a better start. */
			
			  break;
			}
		}

        if (tracer != 0) {     // eject from while loop on trace - AT THE VERY END
		  form.append(trace1s);
		  break;
		}

      }
	  
	  if (trace1s == "") {       // continue trace handling - if you DO NOT trace
		  form.deleteAll(); // will make the runlimit information pointless, unless I save it in a string.
		  
		  if (rlstring != "") {
			form.append(rlstring);
			rlstring = "";
		  } else {
			form.append(" DONE");
		  }
	  } else {                  // This is what you do IF YOU DO TRACE.
		  form.deleteAll();
		  form.append(trace1s);
		  trace1s = "";         // This shows the trace.
		  form.removeCommand(reentry);
		  form.addCommand(runcode);
		  form.setCommandListener(this);
	  }

    } else if (command == exit) {
	  if (previouscommand == submit) {
		form.removeCommand(setvalue);
		form.removeCommand(runcode);
		entryflag = 0;
		form.addCommand(reentry);
		form.setCommandListener(this);
		form.append(" PRESS INIT");       // exit only from main screen.
	  } else if (previouscommand == runcode) {
		form.removeCommand(runcode);
		form.removeCommand(setvalue);
		form.removeCommand(reentry);      // redraw reentry
		form.addCommand(reentry);
		form.setCommandListener(this);
		form.append(" PRESS INIT");
	  } else if (previouscommand == setvalue) {
		form.removeCommand(setvalue);
		form.removeCommand(reentry);      // redraw reentry
		form.addCommand(reentry);
		form.setCommandListener(this);
		form.append(" PRESS INIT");
	  } else {                            // if (previouscommand == reentry) {} Only REALLY exit from the main screen.
        destroyApp(false);
        notifyDestroyed();
	  }
    }
  }
}
