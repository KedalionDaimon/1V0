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

public class IVOnano extends MIDlet implements CommandListener {
  private Display display;

  private Form form = new Form("1V0");
  
  private Command exit = new Command("EXIT", Command.EXIT, 1);

  private Command submit = new Command("ENTER", Command.SCREEN, 1);
  
  private Command reentry = new Command("INIT", Command.SCREEN, 1);
  
  private Command runcode = new Command("RUN", Command.SCREEN, 1);
  
  private Command setvalue = new Command("SET", Command.SCREEN, 1);
  
  private Command previouscommand;

  private TextField cmdfield = new TextField("CMDA:", "0", 4, TextField.ANY); // 3 OR 4

  private TextField oprfield = new TextField("OPER:", "0", 3, TextField.NUMERIC);
  
  private TextField d1_field = new TextField("ADR1:", "1", 4, TextField.NUMERIC);
  
  private TextField d2_field = new TextField("ADR2:", "1", 4, TextField.NUMERIC);
  
  private TextField rl_field = new TextField("NEW RUNLIMIT:", "32767", 5, TextField.NUMERIC);

  private TextField flofield = new TextField("NUM:", "0", 11, TextField.ANY); // Maybe make the text "0000" on fixedpolong!

  private TextField longfield = new TextField("INT:", "0", 11, TextField.ANY);

  private String ephemeral = ""; // I CAN DECLARE HERE ANYTHING AND USE THE STUFF BELOW LIKE GOSUBS
  
  private String rlstring = "";
  
  private String trace1s = "";

  
  	private static long sqrt(long num) {
		num = absvalu(num);
		long guess = num / 4;
		long maxrun = 1;
        long TOLERN = 4;
		long PRECIS = 10000;
		while(absvalu((guess * guess / PRECIS) - num) >= TOLERN) {
			guess = (num * PRECIS/guess + guess) / 2;
			maxrun++;
			if (maxrun > 200) {
				break;
			}
		}
		return guess;
	}
	
	private static long cbrt(long num) {
		long guess = num / 2;
		long maxrun = 1;
		long TOLERN = 4;
		long PRECIS = 10000;
		while(absvalu((guess * guess * guess / (PRECIS * PRECIS)) - num) >= TOLERN) {
			guess = (num * PRECIS/guess + guess) / 2;
			maxrun++;
			if (maxrun > 200) {
				break;
			}
		}
		return guess;
	}
  
	private static long pow(long base, long expo) {
		long ff = 1;
		long ij;
		long PRECIS = 10000;
		if (expo > 1) {
			
			for (ij = 1; ij <= expo; ij++) {
				ff = ff * base / PRECIS;
			}

		} else if (expo < 1) {
			
			expo = -expo;
			
			for (ij = 1; ij <= expo; ij++) {
				ff = ff * PRECIS / base;
			}
			
		} else {
			
			ff = 1;

		}

		return ff;

	}

    private static long absvalu(long x) {
		if (x < 0) {
			return -x;
		} else {
			return x;
		}
	}
	
	/*
	private static long rund(long x) {
		long absv;
		long rst;
		long PRECIS = 10000;
		if (x < 0) {
			absv = -x;
		} else {
			absv = x;
		}
		
		rst = absv % PRECIS;
		
		if (rst >= (PRECIS / 2)) {
			absv = absv - rst + PRECIS;
		} else {
			absv = absv - rst;
		}
		
		if (x < 0) {
			return -absv;
		} else {
			return absv;
		}
	}
	*/

  private int entryflag = 0;

  private int PRECISION = 10000; // How many "decimal places" are counted.
  /* REMEMBER IT INSIDE YOUR CUSTOM FUNCTIONS, TOO */
  
  private int DATAMEM = 1001; // 8191 is the addressable space
  private int EXECMEM = 1001; // ANYTHING YOU LIKE, BUT OTHERWISE YOU WAIT TOO LONG FOR THE END IF YOU DID NOT SPECIFY A JUMP BACK
  // 2501, 1001

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
  private int[] instruction = new int[(int) EXECMEM];
  private int runlimit = 0;
  /* If this is 0, run unlimited, else break when it becomes 1. */
  /* This is a sort of "safety", to prevent infinite looping. */

  private long f = 0; /* f & g are auxiliary longing polong temporary storage */
  private long g = 0;
  private int TOLERANCE = 4; // not "1", this is stupid! Little differences should be accounted for!
  /* ADJUST THE TOLERANCE ALSO INSIDE THE FUNCTIONS BELOW */
  
  private long[] datum = new long[(int) DATAMEM];



  public IVOnano() {

	                                      // Better do stuff BEFORE showing controls.
    for (i=0; i<EXECMEM; i++) {
      instruction[(int) (i)] = 0;
    }

    for (i=0; i<DATAMEM; i++) {
      datum[(int) (i)] = 0;
    }
	
	runlimit = 5 * EXECMEM;
	
	display = Display.getDisplay(this);
    form.append("WELCOME TO 1V0: 0000PRECISION -1show_instructions -2show_data -4enter_data 0NOOP 1JUMP 2IADR 5SADR 6SVAL 7IAAS 8IVAS 9PLUS 10MINS 11MULS 12DIVS 13POXY 17AMNT 19PCNT 22COPY 23FRIS 26CORS 28SUMR 30IXTH 32SQRT 34CBRT 46MSTD 53PLUR 54MINR 55MULR 56DIVR 57PLUN 58MINN 59MULN 60DIVN");
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
	  ephemeral = cmdfield.getString();                        // Turn the field strings longo numbers.
	  cmdadr = java.lang.Integer.parseInt(ephemeral);
	  ephemeral = oprfield.getString();
	  opr = java.lang.Integer.parseInt(ephemeral);
	  ephemeral = d1_field.getString();
	  datadr1 = java.lang.Integer.parseInt(ephemeral);
	  ephemeral = d2_field.getString();
	  datadr2 = java.lang.Integer.parseInt(ephemeral);

      form.removeCommand(submit);        // cycle the available commands
	  for (int delel = form.size() - 1; delel >=0; delel--) { form.delete(delel); }                   // CLS at first safe place

	  if (cmdadr > 0) {
		  
	    for (int delel = form.size() - 1; delel >=0; delel--) { form.delete(delel); }                   // CLS at first safe place
	  
	    // THIS WORKS:
	    ephemeral = Long.toString(cmdadr) + "   " + Long.toString(opr) + "   " + Long.toString(datadr1) + "   " + Long.toString(datadr2);

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
        instruction[(int) (cmdadr)] = instr;

        ephemeral = " ";
            
        ephemeral = ephemeral + cmdadr + ":" + opr;
            
        ephemeral = ephemeral + ":";
        ephemeral = ephemeral + datadr1 + ">" + datum[(int) (datadr1)];
        if ((datum[(int) (datadr1)] > -1 * DATAMEM) && (datum[(int) (datadr1)] < DATAMEM)) {
          j = (int) absvalu((long)(datum[(int) (datadr1)]));
          ephemeral = ephemeral + ">" + datum[(int) (j)];
        }
        ephemeral = ephemeral + ":";
            
        ephemeral = ephemeral + datadr2 + ">" + datum[(int) (datadr2)];
        if ((datum[(int) (datadr2)] > -1 * DATAMEM) && (datum[(int) (datadr2)] < DATAMEM)) {
          j = (int) absvalu((long)(datum[(int) (datadr2)]));
          ephemeral = ephemeral + ">" + datum[(int) (j)];
        }
        ephemeral = ephemeral + ":";
            	
        ephemeral = ephemeral + "DEC:" + instr;
				
		form.append(ephemeral);



        form.addCommand(reentry);          // submit -> reentry
		form.setCommandListener(this);
	  } else if (cmdadr == 0) {
		for (int delel = form.size() - 1; delel >=0; delel--) { form.delete(delel); } 
	    // THIS WORKS:
	    ephemeral = Long.toString(cmdadr) + "   " + Long.toString(opr) + "   " + Long.toString(datadr1) + "   " + Long.toString(datadr2);

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
        instruction[(int) (cmdadr)] = instr;

        ephemeral = " ";
            
        ephemeral = ephemeral + cmdadr + ":" + opr;
            
        ephemeral = ephemeral + ":";
        ephemeral = ephemeral + datadr1 + ">" + datum[(int) (datadr1)];
        if ((datum[(int) (datadr1)] > -1 * DATAMEM) && (datum[(int) (datadr1)] < DATAMEM)) {
          j = (int) absvalu((long)(datum[(int) (datadr1)]));
          ephemeral = ephemeral + ">" + datum[(int) (j)];
        }
        ephemeral = ephemeral + ":";
            
        ephemeral = ephemeral + datadr2 + ">" + datum[(int) (datadr2)];
        if ((datum[(int) (datadr2)] > -1 * DATAMEM) && (datum[(int) (datadr2)] < DATAMEM)) {
          j = (int) absvalu((long)(datum[(int) (datadr2)]));
          ephemeral = ephemeral + ">" + datum[(int) (j)];
        }
        ephemeral = ephemeral + ":";
            	
        ephemeral = ephemeral + "DEC:" + instr;
				
		form.append(ephemeral);

		form.addCommand(runcode);          // GO: submit -> runcode"SUBMIT TO RUN CODE EXIT TO CANCEL"
        form.setCommandListener(this);
		form.append(" ENTER TO RUN, EXIT TO ABORT");
	  } else if (cmdadr == -1) {
		for (int delel = form.size() - 1; delel >=0; delel--) { form.delete(delel); } 
		form.append(d1_field);
		form.append(d2_field);
		form.append(" LIST INSTRUCTION RANGE");
		form.addCommand(setvalue);
		form.setCommandListener(this);
	  } else if (cmdadr == -2) {
		for (int delel = form.size() - 1; delel >=0; delel--) { form.delete(delel); } 
		form.append(d1_field);
		form.append(d2_field);
		form.append(" LIST DATA RANGE");
		form.addCommand(setvalue);
		    form.setCommandListener(this);
	  } else if (cmdadr == -3) {
		for (int delel = form.size() - 1; delel >=0; delel--) { form.delete(delel); } 
		form.append(d1_field);
		form.append(" ENTER INSTRUCTIONS FROM ADDRESS UP");
		form.addCommand(setvalue);
	  } else if (cmdadr == -4) {
		for (int delel = form.size() - 1; delel >=0; delel--) { form.delete(delel); } 
		form.append(d1_field);
		form.append(d2_field);
		form.append(" ENTER NUMBERS IN DATA RANGE");
		form.addCommand(setvalue);
		    form.setCommandListener(this);
	  } else if (cmdadr == -5) {
		for (int delel = form.size() - 1; delel >=0; delel--) { form.delete(delel); } 
		form.append(d1_field);
		form.append(d2_field);
		form.append(" CLEAR INSTRUCTION RANGE");
		form.addCommand(setvalue);
		form.setCommandListener(this);
	  } else if (cmdadr == -6) {
		for (int delel = form.size() - 1; delel >=0; delel--) { form.delete(delel); } 
		form.append(d1_field);
		form.append(d2_field);
		form.append(" CLEAR DATA RANGE");
		form.addCommand(setvalue);
		form.setCommandListener(this);
	  } else if (cmdadr == -7) {
		for (int delel = form.size() - 1; delel >=0; delel--) { form.delete(delel); } 
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
		for (int delel = form.size() - 1; delel >=0; delel--) { form.delete(delel); } 
		form.append(rl_field);
		form.addCommand(setvalue);
		form.setCommandListener(this);
      } else if (cmdadr == -10) {
		for (int delel = form.size() - 1; delel >=0; delel--) { form.delete(delel); } 
		form.addCommand(reentry);          // EJECT: -> reentry
		form.setCommandListener(this);
		form.append(" EXIT FROM INIT MENU");
	  } else {
		for (int delel = form.size() - 1; delel >=0; delel--) { form.delete(delel); } 
		form.addCommand(reentry);          // EJECT: -> reentry
		form.setCommandListener(this);
		form.append(" COMMAND NOT AVAILABLE");
	  }
	  
	} else if (command == reentry) {
	  previouscommand = command;
	  for (int delel = form.size() - 1; delel >=0; delel--) { form.delete(delel); }                   // CLS at first safe place
      form.removeCommand(reentry);       // cycle the available commands
	  form.removeCommand(runcode);       // cycle the available commands
	                                     // Better do stuff BEFORE showing controls.
	  pc = 0;                            // safety: nuke the program counter and the immediate instruction
      instruction[(int) (0)] = 0;

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
		for (int delel = form.size() - 1; delel >=0; delel--) { form.delete(delel); } 
	    ephemeral = longfield.getString();
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
			instruction[(int) (datadr1)] = instr;
			form.append(longfield);
			longfield.setString("0");
			form.addCommand(setvalue);
			form.setCommandListener(this);
			form.append(" ADDRESS: " + datadr1 + ", OPERATION:" + instr + ", ENTER NEXT OPERATION OR 67108863");
			datadr1++;
		}

	  } else if (entryflag == -4) {
		for (int delel = form.size() - 1; delel >=0; delel--) { form.delete(delel); } 
	    ephemeral = flofield.getString();
	    f = java.lang.Long.parseLong(ephemeral);
		datum[(int) (datadr1)] = f;
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
	    for (int delel = form.size() - 1; delel >=0; delel--) { form.delete(delel); } 

		ephemeral = d1_field.getString();
	    datadr1 = java.lang.Integer.parseInt(ephemeral);
	    ephemeral = d2_field.getString();
	    datadr2 = java.lang.Integer.parseInt(ephemeral);

		if ((datadr1 < 0) || (datadr2 < 0)) {                   // fast eject - do nothing
			form.append(" CANCELLED ON NEGATIVE RANGE");
		} else {

			if ((datadr1 == 0) && (datadr2 == 0)) {
			/* prlong EVERYTHING */
				datadr1 = 1;
				datadr2 = DATAMEM - 1;
			} else if (datadr2 == 0) {
				datadr2 = datadr1; /* i.e. prlong only one datum */
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

                instr = instruction[(int) (i)];
                // EEPROM.get((INSTRUCTION_BYTE_LENGTH * (i - 0)) + ZERO_OFFSET, instr);
                xx = (instr >> 26) & 63; /* 4095:255:24:12 or 8191:63:26:13 */
                yy = (instr >> 13) & 8191;
                zz = instr & 8191;
                opr = xx;
                datadr1 = yy;
                datadr2 = zz;
                ephemeral = " ";
            
                ephemeral = ephemeral + i + ":" + opr;
            
                ephemeral = ephemeral + ":";
                ephemeral = ephemeral + datadr1 + ">" + datum[(int) (datadr1)];
                if ((datum[(int) (datadr1)] > -1 * DATAMEM) && (datum[(int) (datadr1)] < DATAMEM)) {
                  j = (int) absvalu((long)(datum[(int) (datadr1)]));
                  ephemeral = ephemeral + ">" + datum[(int) (j)];
                }
                ephemeral = ephemeral + ":";
            
                ephemeral = ephemeral + datadr2 + ">" + datum[(int) (datadr2)];
                if ((datum[(int) (datadr2)] > -1 * DATAMEM) && (datum[(int) (datadr2)] < DATAMEM)) {
                  j = (int) absvalu((long)(datum[(int) (datadr2)]));
                  ephemeral = ephemeral + ">" + datum[(int) (j)];
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
	    for (int delel = form.size() - 1; delel >=0; delel--) { form.delete(delel); } 
		
		ephemeral = d1_field.getString();
	    datadr1 = java.lang.Integer.parseInt(ephemeral);
	    ephemeral = d2_field.getString();
	    datadr2 = java.lang.Integer.parseInt(ephemeral);

		if ((datadr1 < 0) || (datadr2 < 0)) {                   // fast eject - do nothing
			form.append(" CANCELLED ON NEGATIVE RANGE");
		} else {

			if ((datadr1 == 0) && (datadr2 == 0)) {
			/* prlong EVERYTHING */
				datadr1 = 1;
				datadr2 = DATAMEM - 1;
			} else if (datadr2 == 0) {
				datadr2 = datadr1; /* i.e. prlong only one datum */
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
				ephemeral = " " + i + ":" + datum[(int) (i)];
				form.append(ephemeral);
			}

			h = 0;
			k = 0;
			i = 0;
		
		}
		
	  } else if (cmdadr == -3) {
	    for (int delel = form.size() - 1; delel >=0; delel--) { form.delete(delel); } 
	    ephemeral = d1_field.getString();
	    datadr1 = java.lang.Integer.parseInt(ephemeral);

		if ((datadr1 < 0) || (datadr1 > EXECMEM - 1)) {                                 // fast eject - do nothing
			form.append(" CANCELLED ON OUT OF RANGE COMMAND ADDRESS");
		} else {
			form.append(longfield);
			longfield.setString("0");
			form.addCommand(setvalue);
			form.setCommandListener(this);
			entryflag = -3;
		}
  
	  } else if (cmdadr == -4) {
	    for (int delel = form.size() - 1; delel >=0; delel--) { form.delete(delel); } 
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
	    for (int delel = form.size() - 1; delel >=0; delel--) { form.delete(delel); } 
		
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
				instruction[(int) (i)] = instr;
			}
			form.append(" INSTRUCTIONS CLEARED");

			h = 0;
			k = 0;
			i = 0;
		}
		  
	  } else if (cmdadr == -6) {
	    for (int delel = form.size() - 1; delel >=0; delel--) { form.delete(delel); } 
		
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
				datum[(int) (i)] = 0;
			}
			form.append(" DATA CLEARED");

			h = 0;
			k = 0;
			i = 0;
		
		}
	  
	  } else if (cmdadr == -9) {
	    for (int delel = form.size() - 1; delel >=0; delel--) { form.delete(delel); } 
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
    form.setCommandListener(this);
	  }
	  
	} else if (command == runcode) {
	  for (int delel = form.size() - 1; delel >=0; delel--) { form.delete(delel); } 
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

		datum[(int) (0)] = 0; /* Useful for unconditional jumps. */

		if (pc < 0) {
			pc = 0;
		}

// System.out.println("X CMDADR = " + cmdadr + " PC = " + pc + " OPR=" + opr + " DATADR1=" + datadr1 + " DATADR2=" + datadr2 );

		instr = instruction[(int) (pc)];
// System.out.println("DEC INSTRUCTION:" + instr);

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

// System.out.println("Y CMDADR = " + cmdadr + " PC = " + pc + " OPR=" + opr + " DATADR1=" + datadr1 + " DATADR2=" + datadr2 );

		/* TRACE EXECUTION OF EACH PRESENT INSTRUCTION */
		if (tracer != 0) {
			trace1s = " ";
		
			trace1s = trace1s + pc + ":" + opr;
		
			trace1s = trace1s + ":";
			trace1s = trace1s + datadr1 + ">" + datum[(int) (datadr1)];
			if ((datum[(int) (datadr1)] > -1 * DATAMEM) && (datum[(int) (datadr1)] < DATAMEM)) {
			  j = (int) absvalu((long)(datum[(int) (datadr1)]));
			  trace1s = trace1s + ">" + datum[(int) (j)];
			}
			trace1s = trace1s + ":";
		
			trace1s = trace1s + datadr2 + ">" + datum[(int) (datadr2)];
			if ((datum[(int) (datadr2)] > -1 * DATAMEM) && (datum[(int) (datadr2)] < DATAMEM)) {
			  j = (int) absvalu((long)(datum[(int) (datadr2)]));
			  trace1s = trace1s + ">" + datum[(int) (j)];
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

		  /* WHATEVER IS NOT MATCHED: NO OPERATION [NOP)] / RESERVED */
		  /* This particularly applies to "Operation 0", so all starts with 1. */

		  if (opr == 0) {
		  /* NOOP -- NO OPERATION: Do nothing. */
		  /* Any non-implemented opcode will work as NOP. */
		  /* This is also nice if you want to temporarily de-activate certain code. */

		  } else if (opr == 1) {
		  /* JUMP */
		  /* JUMP IF THE FIRST ADDRESS CONTAINS (NEARLY) ZERO OR A NEGATIVE NUMBER*/
		  /* "Nearly" zero, because these are longs and repeated long operations */
		  /* may render the results imprecise. */
		  /* This can very neatly be translated longo a high language's IF & WHILE. */
		  /* Anyway, welcome to "spaghetti programming". You will use this sole */
		  /* condition until you learn to LOVE IT! */

			if (datum[(int) (datadr1)] <= TOLERANCE) {
			  /* pc = (long) absvalu((long)(datum[(int) (datadr2)])); */ /* However, such a default is */
			  /* NOT a good idea, actually: with a default COMPUTED GOTO, you */
			  /* normally straight GOTO HELL, and have NO adequate way to trace it. */
			  /* Instead, let the default be OBVIOUS: */

			  pc = datadr2; 
			  /* On purpose NOT "computed", but IADR still allows it. */
			  /* BEWARE: the program counter adjustment below needed to be RESET, */
			  /* otherwise it was sending you ONE AFTER your desired jump address! */
			} else { pc++; } /* without this, it gets stuck! */
			/* This pc adjustment above makes jumps NOT return to immediate mode: */
			/* pc is set to, essentially, an long indicated by the long value */
			/* at an address. But a return to immediate mode is only possible if */
			/* pc stays 0. This is why a jump can serve as an "entry" to exection. */

		  } else if (opr == 2) {
		  /* IADR -- INDIRECT ADDRESSING: */
		  /* SUBSTITUTE datadr1 AND datadr2 IN THE NEXT INSTRUCTION BY h AND k. */
		  /* Whatever the next operation is, datum[(int) (datadr1)] shall give */
		  /* the next datadr1, and datum[(int) (datadr2)] shall give the next datadr2. */
		  /* This shall serve for a rather powerful "indirect addressing" */
		  /* whenever such is needed, in a flexible manner. */

			h = (int) absvalu((long)(datum[(int) (datadr1)]));
			k = (int) absvalu((long)(datum[(int) (datadr2)]));
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
		  /* This instruction will prlong all data in a range, unless the second */
		  /* address is seither the same as the first address or 0, indicating */
		  /* that only the first address is to be prlonged. */

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

			datum[(int) (datadr1)] = datadr2;
			/* obviously, if datadr2 == datadr1, this will make an address */
			/* "hold itself" */

		  } else if (opr == 6) {
		  /* SVAL -- SET VALUE: This is the "normal 'equal'" in other languages. */
		  /* Did not call it EQUL or thelike, to help visually differentiate */
		  /* values and addresses. */
			datum[(int) (datadr1)] = datum[(int) (datadr2)];

		  } else if (opr == 7) {
		  /* HOW IxAS WORKS: */
		  /* datadr1 contains an address X, i.e. it polongs to it; */
		  /* X is at first expressed as a long to be converted to an long. */
		  /* datum[(int) (X)] shall either be set to an address or a value that is defined */
		  /* in datadr2, i.e. datadr2 itself or the datum at datadr2, respectively. */
		  /* In other words, datum[(int) (datum[(int) (datadr1)])] = datadr2, from now on */
		  /* Indirect Address Assignment or IAAS, or Indirect Value Assignemnt */
		  /* IVAS signifying datum[(int) (datum[(int) (datadr1)])] = datum[(int) (datadr2)]. These may look */
		  /* pretty strange, but they appeared practical for certain loops. */ 

		  /* IAAS */
			i = (int) absvalu((long)(datum[(int) (datadr1)]));
			datum[(int) (i)] = datadr2;

		  } else if (opr == 8) {
		  /* IVAS -- see above. */

			i = (int) absvalu((long)(datum[(int) (datadr1)]));
			datum[(int) (i)] = datum[(int) (datadr2)];

		  /* ---- FROM NOW ON FOLLOW COMMANDS FOR SINGLE NUMERIC MANIPULATION ----- */
		  } else if (opr == 9) {
		  /* PLUS -- PLUS FOR SINGLE NUMBERS, NOT RANGE. */

			datum[(int) (datadr1)] = datum[(int) (datadr1)] + datum[(int) (datadr2)];

		  } else if (opr == 10) {
		  /* MINS -- MINUS FOR SINGLE NUMBERS, NOT RANGE. */

			datum[(int) (datadr1)] = datum[(int) (datadr1)] - datum[(int) (datadr2)];

		  } else if (opr == 11) {
		  /* MULS -- MULTIPLY SINGLE NUMBERS, NOT RANGE. */

			datum[(int) (datadr1)] = datum[(int) (datadr1)] * datum[(int) (datadr2)] / PRECISION;

		  } else if (opr == 12) {
		  /* DIVS -- DIVIDE SINGLE NUMBERS, NOT RANGE. */
		  /* In case of a division by 0, there shall be no hysteria. Just give 0. */
		  /* This makes loops much easier than "having to look out for the */
		  /* mathematical booby-trap" all the time. Here this is done GENERALLY. */

			if (absvalu(datum[(int) (datadr2)]) != 0) {
			  datum[(int) (datadr1)] = datum[(int) (datadr1)] * PRECISION / datum[(int) (datadr2)];
			} else {
			  datum[(int) (datadr1)] = 0;
			}

		  } else if (opr == 13) {
		  /* POXY -- Power Of X: Y, that is, X ^ Y, BASED ON |X| WITH SIGN EX POST. */
		  /* This is a rather powerful instruction, because 1/Y is the Yth root */
		  /* (so Y = 0.5 is a square root, Y= 0.3333 is a cube root, and so on),
		  /* Y = -1 sets X to 1/X, and so forth. - All this operates on the */
		  /* ABSOLUTE VALUE OF X, and preserving the sign, to prevent imaginary */
		  /* number results. Unfortunately, this has the funny effect that -3^2=-9. */

			/* A ^ B =  E ^ (B * ln(A)), but that is not even necessary due to pow. */
			if (datum[(int) (datadr1)] == 0) {
			  datum[(int) (datadr1)] = 0;
			  /* Wanton value because it is undefined for negative exponents */
			  /* and for positive exponents, 0^whatever=0. */
			} else if (datum[(int) (datadr1)] < 0) {
			  datum[(int) (datadr1)] = -1 * (long) pow(absvalu(datum[(int) (datadr1)]), datum[(int) (datadr2)]);
			} else {
			  datum[(int) (datadr1)] = (long) pow(datum[(int) (datadr1)], datum[(int) (datadr2)]);
			}

		  } else if (opr == 22) {
		  /* COPY -- COPY ADDRESS RANGE: */
		  /* Copy longO the range between datadr1 and datadr2, both inclusive, */
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

			/* Determine the SOURCE range h & k that shall be copied longo */
			/* datadr1 and datadr2. (You CAN operate here with h & k, because IADR */
			/* would have taken effect ALREADY, should it have been issued. */
			h = (int) absvalu((long)(datum[(int) (datadr1)]));
			k = (int) absvalu((long)(datum[(int) (datadr2)]));

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
			  datum[(int) (datadr1)] = datum[(int) (h)];
			  /* i.e. datum[(int) (datadr1)] = datum[(int) (datum[(int) (datadr1)])] */
			} else if (h == k) { /* then fill: */
			  /* You can use this to initialise a range to a value. */
			  f = datum[(int) (h)];
			  if (datadr2 > datadr1) {
				for (i = datadr1; i <= datadr2; i++) {
				  datum[(int) (i)] = f;
				}
			  } else {
				for (i = datadr2; i <= datadr1; i++) {
				  datum[(int) (i)] = f;
				}
			  }
			} else if ((datadr2 > datadr1) && (k > h)) {
			  /* The "normal" case - make it revolving instead of "cutting" it. */
			  for (i = datadr1; i <= datadr2; i++) {
				datum[(int) (i)] = datum[(int) (j)];
				j++;
				if (j > k) {
				  j = h;
				}
			  }
			} else if ((datadr2 < datadr1) && (k < h))  {
			  /* The countdown case - here, the revolutions may fill differently. */
			  for (i = datadr1; i >= datadr2; i--) {
				datum[(int) (i)] = datum[(int) (j)];
				j--;
				if (j < k) {
				  j = h;
				}
			  }
			} else if ((datadr2 < datadr1) && (k > h)) {
			  /* this and the next will "reverse" a range of equal size */
			  /* they really only differ in how they would handle revolutions */
			  for (i = datadr1; i >= datadr2; i--) {
				datum[(int) (i)] = datum[(int) (j)];
				j++;
				if (j > k) {
				  j = h;
				}
			  }
			} else if ((datadr2 > datadr1) && (k < h)) {
			  for (i = datadr1; i <= datadr2; i++) {
				datum[(int) (i)] = datum[(int) (j)];
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
		  /* This is like looping a[n + 1)] = a[n)] + x.*/
		  /* datadr1 and datadr2 describe the range to be filled, */
		  /* datadr1 holds the initial value and datadr2 holds the step. */

			if ((datadr1 == datadr2) || (datadr2 == 0)) {
			  /* Do nothing - datadr1 has the init value, done!  */
			} else if (datadr1 < datadr2) {
			  f = datum[(int) (datadr2)];
			  for (i = datadr1 + 1; i <= datadr2; i++) {
			  /* datadr1 + 1 as datadr1 already contains the initial value */
				datum[(int) (i)] = datum[(int) (i - 1)] + f;
			  }
			} else if (datadr1 > datadr2) { /* then do a count-down */
			  f = datum[(int) (datadr2)];
			  for (i = datadr1 - 1; i >= datadr2; i--) {
				datum[(int) (i)] = datum[(int) (i + 1)] - f;
			  }
			}
			/* Giving a step-value of zero, this of course will just copy */
			/* the initial value onto an entire range. An initial value of zero */
			/* and a step of zero can be thus used to erase a memory section. */


		  } else if (opr == 17) {
		  /* NEXT THREE INSTRUCTIONS: PERCENT CALCULATIONS OVER TIME. */
		  /* Each instruction is named for what is SOUGHT: */
		  /* amount, period or percentage, assuming that */
		  /* amount = (1 + (percentage / 100)) ^ period. */

		  /* AMNT -- Find the amount, whereby */
		  /* datadr1 contains the periods */
		  /* datadr2 contains the percent (not as, say, 0.25, but as 25). */

			f = datum[(int) (datadr1)];
			g = datum[(int) (datadr2)];
			g = (long) pow(1 * PRECISION + (g / 100), f); // g * PRECISION / 100 * PRECISION MEANS NO CHANGE NEEDED
			datum[(int) (datadr1)] = g;
			/* you overwrite the period, but keep the percent untouched */
			f = 0;
			g = 0;


		  } else if (opr == 19) {
		  /* PCNT -- Find the percent, whereby */
		  /* datadr1 contains the final amount */
		  /* datadr2 contains the period */

			f = datum[(int) (datadr1)];
			g = datum[(int) (datadr2)];
			g = 1/g;
			g = 100 * ((long) pow(f, g) - 1 * PRECISION);
			datum[(int) (datadr1)] = g;
			/* you overwrite the final amount, but keep the period untouched */
			f = 0;
			g = 0;




		  } else if (opr == 26) {
		  /* CORS - COUPLED RANGE SORTING: The first range, starting from */
		  /* datadr1 + 1 and continuing to datum[(int) (datadr1)], both inclusive, */
		  /* determines how datadr2 + 1 till datum[(int) (datadr2)] is sorted. */
		  /* datadr1 is used as the "sorting index" for datadr2. */
		  /* This is very much like the general sorting routine. */
		  /* What this is for: to sort something "according to" something else. */
		  /* This should help solve "best"-value-tasks. */
		  /* If datadr2 is determined to be 0, then CORS really works like SORT, */
		  /* sorting the first range datum[(int) (datadr1 + 1)] ... datum[(int) (datum[(int) (datadr1)])]. */

			h = (int) absvalu((long)(datum[(int) (datadr1)])); /* Only datum[(int) (datadr1)] deterines range. */
			/* k is used for flagging only, and not for range determination, and */
			/* instead it is assumed that the range in datadr2 is of the same */
			/* nature as the range signified by datadr1; in other words, */
			/* datum[(int) (datadr2)] is simply ignored. This is not a disadvantage - this */
			/* way, a range can be secondarily sorted that begins at datum[(int) (1)]. */

			if ((datadr1 == h) || (datadr1 == h + 1) || (datadr1 == h - 1)) {
			/* DO NOTHING AT ALL 0 or 1 element "range" is not to be "sorted". */
			} else if (datadr1 == h - 2) {

			  if (datum[(int) (datadr1 + 1)] > datum[(int) (datadr1 + 2)]) {
				f = datum[(int) (datadr1 + 2)];
				datum[(int) (datadr1 + 2)] = datum[(int) (datadr1 + 1)];
				datum[(int) (datadr1 + 1)] = f;
				/* Do the same thing in the other range */
				if (datadr2 != 0) {
				  f = datum[(int) (datadr2 + 2)];
				  datum[(int) (datadr2 + 2)] = datum[(int) (datadr2 + 1)];
				  datum[(int) (datadr2 + 1)] = f;
				}
			  }

			} else if (datadr1 == h + 2) {

			  if (datum[(int) (datadr1 - 2)] > datum[(int) (datadr1 - 1)]) {
				f = datum[(int) (datadr1 - 2)];
				datum[(int) (datadr1 - 2)] = datum[(int) (datadr1 - 1)];
				datum[(int) (datadr1 - 1)] = f;
				/* Do the same thing in the other range */
				if (datadr2 != 0) {
				  f = datum[(int) (datadr2 + 2)];
				  datum[(int) (datadr2 + 2)] = datum[(int) (datadr2 + 1)];
				  datum[(int) (datadr2 + 1)] = f;
				}
			  }

			} else if (datadr1 + 1 < h) {
			  k = 1; /* flag: do we have to sort */
			  while (k == 1) {
				k = 0; /* try to say you are done */
				for (i = datadr1 + 1; i < h; i++) {
				  if (datum[(int) (i)] > datum[(int) (i + 1)]) {
					f = datum[(int) (i + 1)];
					datum[(int) (i + 1)] = datum[(int) (i)];
					datum[(int) (i)] = f;
					k = 1; /* still work to do */
					if (datadr2 != 0) {
					  j = i - datadr1; /* determine how the offset from datadr1 ... */
					  j = j + datadr2; /* ... and apply it to datadr2 ... */
					 /* ... using it to do the same thing in the other range */
					  f = datum[(int) (j + 1)];
					  datum[(int) (j + 1)] = datum[(int) (j)];
					  datum[(int) (j)] = f;
					}
				  }
				}
			  }
			} else if (datadr1 + 1 > h) {
			  k = 1; /* flag: do we have to sort */
			  while (k == 1) {
				k = 0; /* try to say you are done */
				for (i = h + 1; i < datadr1; i++) {
				  if (datum[(int) (i)] < datum[(int) (i + 1)]) {
					f = datum[(int) (i + 1)];
					datum[(int) (i + 1)] = datum[(int) (i)];
					datum[(int) (i)] = f;
					k = 1; /* still work to do */
					if (datadr2 != 0) {
					  j = i - datadr1; /* determine how the offset from datadr1 ... */
					  j = j + datadr2; /* ... and apply it to datadr2 ... */
					 /* ... using it to do the same thing in the other range */
					  f = datum[(int) (j + 1)];
					  datum[(int) (j + 1)] = datum[(int) (j)];
					  datum[(int) (j)] = f;
					}
				  }
				}
			  }
			}

			h = 0;
			k = 0;


		  } else if (opr == 28) {
		  /* SUMR -- SUM OF ADDRESS RANGE - RESULT ALWAYS IN FIRST ADDRESS. */
		  /* Sum up the range signified by datum[(int) (datum[(int) (datadr1)])] and */
		  /* datum[(int) (datum[(int) (datadr2)])], and place the result longo datum[(int) (datadr1)]. */
			h = (int) absvalu((long)(datum[(int) (datadr1)]));
			k = (int) absvalu((long)(datum[(int) (datadr2)]));
			if ((h == k) || (k == 0)) { /* nothing to sum up if only one number */
				datum[(int) (datadr1)] = datum[(int) (h)];
			} else if (h < k) {
			  datum[(int) (datadr1)] = 0;
			  for (i = h; i <= k; i++) {
				datum[(int) (datadr1)] = datum[(int) (datadr1)] + datum[(int) (i)];
			  }
			} else if (h > k) {
			  datum[(int) (datadr1)] = 0;
			  for (i = k; i <= h; i++) {
				datum[(int) (datadr1)] = datum[(int) (datadr1)] + datum[(int) (i)];
			  }
			}
			h = 0;
			k = 0;



		  } else if (opr == 30) {
		  /* The next commands focus on a SINGLE range - and often have */
		  /* meaning even if used on a single number, by setting datadr2 to */
		  /* the same value as datadr1 or to zero (which is shorthand for that). */
		  /* This is in contrast to actual range-range-commands, which */
		  /* operate on TWO ranges, not on ONE range. */

		  /* IXTH -- turn each X longo 1/Xth in a range. X=0 gives 0, as always.  */
		  /* The range is signified by datadr1 and datadr2, and if these are equal, */
		  /* or if datadr2 is zero, then just perform that operation on the first */
		  /* number, i.e. on datum[(int) (datadr1)]. */
			if ((datadr1 == datadr2) || (datadr2 == 0)) {
				if (datum[(int) (datadr1)] != 0) {
				  datum[(int) (datadr1)] = 1 * PRECISION/datum[(int) (datadr1)];
				} else {
				  datum[(int) (datadr1)] = 0;
				}
			} else if (datadr1 < datadr2) {
			  for (i = datadr1; i <= datadr2; i++) {
				if (datum[(int) (i)] != 0) {
				  datum[(int) (i)] = 1 * PRECISION/datum[(int) (i)];
				} else {
				  datum[(int) (i)] = 0;
				}
			  }
			} else if (datadr1 > datadr2) {
			  for (i = datadr2; i <= datadr1; i++) {
				if (datum[(int) (i)] != 0) {
				  datum[(int) (i)] = 1 * PRECISION/datum[(int) (i)];
				} else {
				  datum[(int) (i)] = 0;
				}
			  }
			}

		  } else if (opr == 32) {
		  /* SQRT -- SQUARE ROOT ABSOLUTES IN RANGE */
		  /* The range is signified by datadr1 and datadr2, and if these are equal, */
		  /* or if datadr2 is zero, then just perform that operation on the first */
		  /* number, i.e. on datum[(int) (datadr1)]. Same principle as above. */

			if ((datadr1 == datadr2) || (datadr2 == 0)) {
				datum[(int) (datadr1)] = (long) sqrt(absvalu(datum[(int) (datadr1)]));
			} else if (datadr1 < datadr2) {
			  for (i = datadr1; i <= datadr2; i++) {
				datum[(int) (i)] = (long) sqrt(absvalu(datum[(int) (i)]));
			  }
			} else if (datadr1 > datadr2) {
			  for (i = datadr2; i <= datadr1; i++) {
				datum[(int) (i)] = (long) sqrt(absvalu(datum[(int) (i)]));
			  }
			}

		  } else if (opr == 34) {
		  /* CBRT -- CUBE ROOT IN RANGE */
		  /* The range is signified by datadr1 and datadr2, and if these are equal, */
		  /* or if datadr2 is zero, then just perform that operation on the first */
		  /* number, i.e. on datum[(int) (datadr1)]. Same principle as above. */

			if ((datadr1 == datadr2) || (datadr2 == 0)) {
				datum[(int) (datadr1)] = (long) cbrt(datum[(int) (datadr1)]);
			} else if (datadr1 < datadr2) {
			  for (i = datadr1; i <= datadr2; i++) {
				datum[(int) (i)] = (long) cbrt(datum[(int) (i)]);
			  }
			} else if (datadr1 > datadr2) {
			  for (i = datadr2; i <= datadr1; i++) {
				datum[(int) (i)] = (long) cbrt(datum[(int) (i)]);
			  }
			}

		  } else if (opr == 46) {
		  /* MSTD -- MEAN AND STANDARD DEVIATION ON A SAMPLE. */
		  /* I.e. this is WITH "Bessel's correction" for the standard deviation. */
		  /* A statistical function. datadr1 and datadr2 indicate a range, */
		  /* datum[(int) (datum[(int) (datadr1)])] till datum[(int) (datum[(int) (datadr2)])]. After application, */
		  /* the mean of the range is contained in datadr1 and the standard */
		  /* deviation in datadr2. A Gaussian distribution is assumed. */

    			h = (int) absvalu((long)(datum[(int) (datadr1)]));
    			k = (int) absvalu((long)(datum[(int) (datadr2)]));
    			j = (int) absvalu(h - k); /* how many cells minus 1*/
                   
                       if ((h == k) || (k == 0)) { /* "very funny" - no range */
                         datum[(int) (datadr1)] = datum[(int) (h)]; /* No standard deviation can be given. */
                       } else if (h < k) {
                         datum[datadr1] = 0;
                         for (i = h; i <= k; i++) {
                           datum[(int) (datadr1)] = datum[(int) (datadr1)] + datum[(int) (i)];
                         }
                       } else if (h > k) {
                         datum[datadr1] = 0;
                         for (i = k; i <= h; i++) {
                           datum[(int) (datadr1)] = datum[(int) (datadr1)] + datum[(int) (i)];
                         }
                       }
                   
                       datum[(int) (datadr1)] = datum[(int) (datadr1)]/(j + 1); /* average or mean */;
                   
                       if (datadr2 != datadr1) {
                         if ((h == k) || (k == 0)) { /* "very funny" - no range */
                           datum[(int) (datadr2)] = 0;
                           /* No standard deviation can be given. */
                         } else if (h < k) {
                           datum[(int) (datadr2)] = 0;
                           for (i = h; i <= k; i++) {
                             datum[(int) (datadr2)] = datum[(int) (datadr2)]
                               + ((datum[(int) (i)] - datum[(int) (datadr1)])
                                 * (datum[(int) (i)] - datum[(int) (datadr1)]));
                           }
                         } else if (h > k) {
                           datum[(int) (datadr2)] = 0;
                           for (i = k; i <= h; i++) {
                             datum[(int) (datadr2)] = datum[(int) (datadr2)]
                               + ((datum[(int) (i)] - datum[(int) (datadr1)])
                                 * (datum[(int) (i)] - datum[(int) (datadr1)]));
                           }
                         }

                         datum[(int) (datadr2)] = (long) sqrt(datum[(int) (datadr2)]/j); /* standard deviation */
                         /* Note that "j" is quasi "n-1" in the usual mathematical notation. */
                       }
                       h = 0;
                       k = 0;
			
		  /*
		  } else if (opr == 49) {
		  // RUND -- RANGE.

		  // This attempts to turn decimals into integers.

			i = datadr1;
			if ((datadr1 == datadr2) || (datadr2 == 0)) {
				datum[(int) (i)] = rund(datum[(int) (i)]);
			} else if (datadr1 < datadr2) {
			  for (i = datadr1; i <= datadr2; i++) {
				datum[(int) (i)] = rund(datum[(int) (i)]);
			  }
			} else if (datadr1 > datadr2) {
			  for (i = datadr2; i <= datadr1; i++) {
				datum[(int) (i)] = rund(datum[(int) (i)]);
			  }
			}
			*/


		  /* ---- FROM NOW ON FOLLOW RANGE-WITH-RANGE-OPERATIONS ------------------ */
		  } else if (opr == 53) {
		  /* RANGE-RANGE-COMMANDS (AND RANGE-NUMBER-COMMANDS): */

		  /* Essentially, with ranges, do something for each corresponding position */
		  /* in two ranges, and end when the shorter range has finished. - A number */
		  /* to range application means to modify each position in a range by said */
		  /* number in the manner specified by the respective operator. */

		  /* The first address will be one BEFORE or the one AFTER the range, and */
		  /* it will contain the polonger to the end (or beginning) of the */
		  /* respective range. That is: A[-1)] --> An ; B[-1)] --> Bn. */
		  /* The result will be in the first range. */
		  /* To keep things simple, if the second address is NOT */
		  /* higher or equal to the first address, simply DO NOTHING. */

		  /* I was totally unsure shouldn't I use datum[(int) (0)] as "accumulator" here, */
		  /* but it would make tracing errors more difficult, as datum[(int) (0)] would get */
		  /* all the time overwritten. But e.g. supplying the two addresses and */
		  /* have datum[(int) (0)] supply the length would be totally feasible, */
		  /* as an alternative - just then avoid "nuking" it with 0 all the time. */
		  /* However, this would NOT make it easier for the targeted user base, */
		  /* and "ease of use by the user" is a design priority: without this */
		  /* permanent overwriting, the "accumulator" of each range may be prlonged */
		  /* to see actually where you may have messed up, not as with datum[(int) (0)]. */
		  /* For the user, the advice is to leave at least one number BELOW and */
		  /* at least one number ABOVE each longeresting range unused, so as to */
		  /* be able to use range commands (both top-down and bottom-up). */

		  /* datum[(int) (datadr1 + 1)] is the range begin */
		  /* datum[(int) (h)] is the range end; for k & datadr2, respectively */
		  /* So each material range actually begins NOT at datadr1 and datadr2, but */
		  /* at one address HIGHER than them. */

		  /* PLUR -- ADD TWO RANGES. */
		  /* If they are not of equal length, the operation will stop with the end */
		  /* of the shorter range. */

			h = (int) absvalu((long)(datum[(int) (datadr1)]));
			k = (int) absvalu((long)(datum[(int) (datadr2)]));
			if ((h == datadr1) || (k == datadr2)) {
			  /* then simply do nothing, there is exactly NO range to operate on. */
			} else if ((h > datadr1) && (k > datadr2)) {
			  j = datadr2 + 1;
			  for (i = datadr1 + 1; ((i <= h) && (j <= k)); i++) {
				datum[(int) (i)] = datum[(int) (i)] + datum[(int) (j)];
				j++;
			  }
			} else if ((h < datadr1) && (k < datadr2)) {
			  /* still, datadr have the ADDRESSES and cannot be used themselves */
			  j = datadr2 - 1;
			  for (i = datadr1 - 1; ((i >= h) && (j >= k)); i--) {
				datum[(int) (i)] = datum[(int) (i)] + datum[(int) (j)];
				j--;
			  }
			} else if ((h > datadr1) && (k < datadr2)) {
			  /* the mixed modes give you a "reversed array" operation */
			  j = datadr2 - 1;
			  for (i = datadr1 + 1; ((i <= h) && (j >= k)); i++) {
				datum[(int) (i)] = datum[(int) (i)] + datum[(int) (j)];
				j--;
			  }
			} else if ((h < datadr1) && (k > datadr2)) {
			  /* the mixed modes give you a "reversed array" operation */
			  j = datadr2 + 1;
			  for (i = datadr1 - 1; ((i >= h) && (j <= k)); i--) {
				datum[(int) (i)] = datum[(int) (i)] + datum[(int) (j)];
				j++;
			  }
			}

			h = 0;
			k = 0;

		  } else if (opr == 54) {
		  /* MINR -- SUBTRACT A RANGE FROM A RANGE */
		  /* If they are not of equal length, the operation will stop with the end */
		  /* of the shorter range. */

			h = (int) absvalu((long)(datum[(int) (datadr1)]));
			k = (int) absvalu((long)(datum[(int) (datadr2)]));
			if ((h == datadr1) || (k == datadr2)) {
			  /* then simply do nothing, there is exactly NO range to operate on. */
			} else if ((h > datadr1) && (k > datadr2)) {
			  j = datadr2 + 1;
			  for (i = datadr1 + 1; ((i <= h) && (j <= k)); i++) {
				datum[(int) (i)] = datum[(int) (i)] - datum[(int) (j)];
				j++;
			  }
			} else if ((h < datadr1) && (k < datadr2)) {
			  /* still, datadr have the ADDRESSES and cannot be used themselves */
			  j = datadr2 - 1;
			  for (i = datadr1 - 1; ((i >= h) && (j >= k)); i--) {
				datum[(int) (i)] = datum[(int) (i)] - datum[(int) (j)];
				j--;
			  }
			} else if ((h > datadr1) && (k < datadr2)) {
			  /* the mixed modes give you a "reversed array" operation */
			  j = datadr2 - 1;
			  for (i = datadr1 + 1; ((i <= h) && (j >= k)); i++) {
				datum[(int) (i)] = datum[(int) (i)] - datum[(int) (j)];
				j--;
			  }
			} else if ((h < datadr1) && (k > datadr2)) {
			  /* the mixed modes give you a "reversed array" operation */
			  j = datadr2 + 1;
			  for (i = datadr1 - 1; ((i >= h) && (j <= k)); i--) {
				datum[(int) (i)] = datum[(int) (i)] - datum[(int) (j)];
				j++;
			  }
			}

			h = 0;
			k = 0;

		  } else if (opr == 55) {
		  /* MULR -- MULTIPLY A RANGE WITH A RANGE */
		  /* If they are not of equal length, the operation will stop with the end */
		  /* of the shorter range. */

			h = (int) absvalu((long)(datum[(int) (datadr1)]));
			k = (int) absvalu((long)(datum[(int) (datadr2)]));
			if ((h == datadr1) || (k == datadr2)) {
			  /* then simply do nothing, there is exactly NO range to operate on. */
			} else if ((h > datadr1) && (k > datadr2)) {
			  j = datadr2 + 1;
			  for (i = datadr1 + 1; ((i <= h) && (j <= k)); i++) {
				datum[(int) (i)] = datum[(int) (i)] * datum[(int) (j)] / PRECISION;
				j++;
			  }
			} else if ((h < datadr1) && (k < datadr2)) {
			  /* still, datadr have the ADDRESSES and cannot be used themselves */
			  j = datadr2 - 1;
			  for (i = datadr1 - 1; ((i >= h) && (j >= k)); i--) {
				datum[(int) (i)] = datum[(int) (i)] * datum[(int) (j)] / PRECISION;
				j--;
			  }
			} else if ((h > datadr1) && (k < datadr2)) {
			  /* the mixed modes give you a "reversed array" operation */
			  j = datadr2 - 1;
			  for (i = datadr1 + 1; ((i <= h) && (j >= k)); i++) {
				datum[(int) (i)] = datum[(int) (i)] * datum[(int) (j)] / PRECISION;
				j--;
			  }
			} else if ((h < datadr1) && (k > datadr2)) {
			  /* the mixed modes give you a "reversed array" operation */
			  j = datadr2 + 1;
			  for (i = datadr1 - 1; ((i >= h) && (j <= k)); i--) {
				datum[(int) (i)] = datum[(int) (i)] * datum[(int) (j)] / PRECISION;
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

			h = (int) absvalu((long)(datum[(int) (datadr1)]));
			k = (int) absvalu((long)(datum[(int) (datadr2)]));
			if ((h == datadr1) || (k == datadr2)) {
			  /* then simply do nothing, there is exactly NO range to operate on. */
			} else if ((h > datadr1) && (k > datadr2)) {
			  j = datadr2 + 1;
			  for (i = datadr1 + 1; ((i <= h) && (j <= k)); i++) {
				if (datum[(int) (j)] != 0) {
				  datum[(int) (i)] = datum[(int) (i)] * PRECISION / datum[(int) (j)];
				} else {
				  datum[(int) (i)] = 0;
				}
				j++;
			  }
			} else if ((h < datadr1) && (k < datadr2)) {
			  /* still, datadr have the ADDRESSES and cannot be used themselves */
			  j = datadr2 - 1;
			  for (i = datadr1 - 1; ((i >= h) && (j >= k)); i--) {
				if (datum[(int) (j)] != 0) {
				  datum[(int) (i)] = datum[(int) (i)] * PRECISION / datum[(int) (j)];
				} else {
				  datum[(int) (i)] = 0;
				}
				j--;
			  }
			} else if ((h > datadr1) && (k < datadr2)) {
			  /* the mixed modes give you a "reversed array" operation */
			  j = datadr2 - 1;
			  for (i = datadr1 + 1; ((i <= h) && (j >= k)); i++) {
				if (datum[(int) (j)] != 0) {
				  datum[(int) (i)] = datum[(int) (i)] * PRECISION / datum[(int) (j)];
				} else {
				  datum[(int) (i)] = 0;
				}
				j--;
			  }
			} else if ((h < datadr1) && (k > datadr2)) {
			  /* the mixed modes give you a "reversed array" operation */
			  j = datadr2 + 1;
			  for (i = datadr1 - 1; ((i >= h) && (j <= k)); i--) {
				if (datum[(int) (j)] != 0) {
				  datum[(int) (i)] = datum[(int) (i)] * PRECISION / datum[(int) (j)];
				} else {
				  datum[(int) (i)] = 0;
				}
				j++;
			  }
			}

			h = 0;
			k = 0;

		  /* ---- FROM NOW ON FOLLOW RANGE-WITH-SINGLE-NUMBER-OPERATIONS ---------- */
		  } else if (opr == 57) {
		  /* PLUN -- ADD A NUMBER TO A RANGE. */

			h = (int) absvalu((long)(datum[(int) (datadr1)]));
			if (h == datadr1) {
			  /* then simply do nothing, there is exactly NO range to operate on. */
			} else if (h > datadr1) {
			  for (i = datadr1 + 1; i <= h; i++) {
				datum[(int) (i)] = datum[(int) (i)] + datum[(int) (datadr2)];
			  }
			} else if (h < datadr1) {
			  for (i = datadr1 - 1; i >= h; i--) {
				datum[(int) (i)] = datum[(int) (i)] + datum[(int) (datadr2)];
			  }
			}

			h = 0;
			k = 0;

		  } else if (opr == 58) {
		  /* MINN -- SUBTRACT A NUMBER FROM A RANGE. */

			h = (int) absvalu((long)(datum[(int) (datadr1)]));
			if (h == datadr1) {
			  /* then simply do nothing, there is exactly NO range to operate on. */
			} else if (h > datadr1) {
			  for (i = datadr1 + 1; i <= h; i++) {
				datum[(int) (i)] = datum[(int) (i)] - datum[(int) (datadr2)];
			  }
			} else if (h < datadr1) {
			  for (i = datadr1 - 1; i >= h; i--) {
				datum[(int) (i)] = datum[(int) (i)] - datum[(int) (datadr2)];
			  }
			}

			h = 0;
			k = 0;

		  } else if (opr == 59) {
		  /* MULN -- MULTIPLY A RANGE WITH A NUMBER. */

			h = (int) absvalu((long)(datum[(int) (datadr1)]));
			if (h == datadr1) {
			  /* then simply do nothing, there is exactly NO range to operate on. */
			} else if (h > datadr1) {
			  for (i = datadr1 + 1; i <= h; i++) {
				datum[(int) (i)] = datum[(int) (i)] * datum[(int) (datadr2)] / PRECISION;
			  }
			} else if (h < datadr1) {
			  for (i = datadr1 - 1; i >= h; i--) {
				datum[(int) (i)] = datum[(int) (i)] * datum[(int) (datadr2)] / PRECISION;
			  }
			}

			h = 0;
			k = 0;


		  } else if (opr == 60) {
		  /* DIVN -- DIVIDE A RANGE BY A NUMBER. */
		  /* A division by 0 nullifies the dividend, */
		  /* as before on single-number-operations. */

			h = (int) absvalu((long)(datum[(int) (datadr1)]));
			if (h == datadr1) {
			  /* then simply do nothing, there is exactly NO range to operate on. */
			} else if (h > datadr1) {
			  if (datum[(int) (datadr2)] != 0) {
				for (i = datadr1 + 1; i <= h; i++) {
				  datum[(int) (i)] = datum[(int) (i)] * PRECISION / datum[(int) (datadr2)];
				}
			  } else {
				for (i = datadr1 + 1; i <= h; i++) {
				  datum[(int) (i)] = 0;
				}
			  }
			} else if (h < datadr1) {
			  if (datum[(int) (datadr2)] != 0) {
				for (i = datadr1 - 1; i >= h; i--) {
				  datum[(int) (i)] = datum[(int) (i)] * PRECISION / datum[(int) (datadr2)];
				}
			  } else {
				for (i = datadr1 - 1; i >= h; i--) {
				  datum[(int) (i)] = 0;
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
			/* whereby a jump will change pc and then go on - your entry polong is */
			/* thus freely selectable. A program can be set for automatic execution */
			/* perhaps by writing in instruction[(int) (0)] a jump to the entry polong, but */
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
		  for (int delel = form.size() - 1; delel >=0; delel--) { form.delete(delel); }  // will make the runlimit information polongless, unless I save it in a string.
		  
		  if (rlstring != "") {
			form.append(rlstring);
			rlstring = "";
		  } else {
			form.append(" DONE");
		  }
	  } else {                  // This is what you do IF YOU DO TRACE.
		  for (int delel = form.size() - 1; delel >=0; delel--) { form.delete(delel); } 
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
		form.append(" PRESS INIT");       // exit only from main screen.
		form.setCommandListener(this);
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
