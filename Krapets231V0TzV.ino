/*
Connection Table

ATTiny85 board : TM1638

GND : GND
5V : VCC
STB : P0
CLK : P1
DIO : P2

Power may be supplied through the ATTiny 85 board, VIN & GND with a 9V battery clip.
*/

/* Tzvet1V0 1V0 TZ V */

#include <EEPROM.h> /* https://github.com/PaulStoffregen/EEPROM */

const int strobe = 0;
const int clock = 1;
const int data = 2;
uint8_t numdecomposed[10];
uint8_t numbermatrix[11][7];

/* These DATAMEM and EXECMEM hold data and instructions, respectively. */
#define DATAMEM 74
#define SPEEDVAR 73
#define SPEEDDELAY 250

/* But the above REQUIRES IADR jumps above 63, a frequent source of errors. */
/* You can activate it freely without any penalty, except said errors! */
/* If you want to jump anywhere without having to use IADR: */
#define EXECMEM 128

/* I.e. using 964 bytes of EEPROM, leaving you with 60 bytes or 15 shifts   */
/* If any "location 0" lasts you a year or two, you should be good for some */
/* 10-20 years or so of usage. Yeah, go ahead, I dare you! */

#define INSTRUCTION_BYTE_LENGTH 4

long datum[DATAMEM];

long instr = 0;
/* instruction format: */
/* operator, data address 1 (=result), data address 2 */
/* every operator is at a command address. */
int8_t cmdadr = 0;
int opr = 0;
int datadr1 = 0;
int datadr2 = 0;
int datadr3 = 0;
unsigned long ww = 0;
unsigned long xx = 0;
unsigned long yy = 0;
unsigned long zz = 0;

int pc = 0;
int h = 0; /* beware: h & k are used for "transcendental" instructions ... */
int i = 0;     /* i & j are auxiliary looping variables */
int j = 0;
int k = 0; /* ... so ZERO THEM OUT after non-transcendental usage */
int l = 0;

unsigned int runlimit = 0;
/* If this is 0, run unlimited, else break when it becomes 1. */
/* This is a sort of "safety", to prevent infinite looping. */

void tmreset() {
  digitalWrite(strobe, LOW);
  shiftOut(data, clock, LSBFIRST, 0x40);
  digitalWrite(strobe, HIGH);

  digitalWrite(strobe, LOW);
  shiftOut(data, clock, LSBFIRST, 0xc0);
  for (uint8_t i = 0; i < 16; i++)
  {
    shiftOut(data, clock, LSBFIRST, 0x00);
    shiftOut(data, clock, LSBFIRST, 255);
  }
  digitalWrite(strobe, HIGH);
}

void displaynumber(int32_t nmbr) {
  tmreset();

  int8_t nuke;
  int32_t delmul;
  uint8_t digitnbr;
  uint8_t looopeer;

  for (nuke = 1; nuke < 10; nuke++) {
    numdecomposed[nuke] = 0;
  }
  if (nmbr < 0) {
    numdecomposed[9] = 1; // i.e. it is negative, for minus-sign
    nmbr = -nmbr; // make it positive
  }
  for (looopeer = 0; looopeer < 9; looopeer++) {
    if (nmbr == 0) {
      if (looopeer > 0) {
        break;
      }
    }
    delmul = nmbr / 10;
    delmul = delmul * 10;
    digitnbr = nmbr - delmul;
    if (digitnbr == 0) {
      digitnbr = 10;
    }
    numdecomposed[looopeer] = digitnbr;
    nmbr = nmbr / 10;
  }

  numbermatrix[0][0] = 0;
  numbermatrix[0][1] = 0;
  numbermatrix[0][2] = 0;
  numbermatrix[0][3] = 0;
  numbermatrix[0][4] = 0;
  numbermatrix[0][5] = 0;
  numbermatrix[0][6] = 0;
  numbermatrix[10][0] = 240;
  numbermatrix[10][1] = 242;
  numbermatrix[10][2] = 244;
  numbermatrix[10][3] = 246;
  numbermatrix[10][4] = 248;
  numbermatrix[10][5] = 250;
  numbermatrix[10][6] = 0;
  numbermatrix[1][0] = 242;
  numbermatrix[1][1] = 244;
  numbermatrix[1][2] = 0;
  numbermatrix[1][3] = 0;
  numbermatrix[1][4] = 0;
  numbermatrix[1][5] = 0;
  numbermatrix[1][6] = 0;
  numbermatrix[2][0] = 240;
  numbermatrix[2][1] = 242;
  numbermatrix[2][2] = 252;
  numbermatrix[2][3] = 248;
  numbermatrix[2][4] = 246;
  numbermatrix[2][5] = 0;
  numbermatrix[2][6] = 0;
  numbermatrix[3][0] = 240;
  numbermatrix[3][1] = 242;
  numbermatrix[3][2] = 252;
  numbermatrix[3][3] = 244;
  numbermatrix[3][4] = 246;
  numbermatrix[3][5] = 0;
  numbermatrix[3][6] = 0;
  numbermatrix[4][0] = 250;
  numbermatrix[4][1] = 252;
  numbermatrix[4][2] = 242;
  numbermatrix[4][3] = 244;
  numbermatrix[4][4] = 0;
  numbermatrix[4][5] = 0;
  numbermatrix[4][6] = 0;
  numbermatrix[5][0] = 240;
  numbermatrix[5][1] = 250;
  numbermatrix[5][2] = 252;
  numbermatrix[5][3] = 244;
  numbermatrix[5][4] = 246;
  numbermatrix[5][5] = 0;
  numbermatrix[5][6] = 0;
  numbermatrix[6][0] = 240;
  numbermatrix[6][1] = 250;
  numbermatrix[6][2] = 248;
  numbermatrix[6][3] = 246;
  numbermatrix[6][4] = 244;
  numbermatrix[6][5] = 252;
  numbermatrix[6][6] = 0;
  numbermatrix[7][0] = 250;
  numbermatrix[7][1] = 240;
  numbermatrix[7][2] = 242;
  numbermatrix[7][3] = 244;
  numbermatrix[7][4] = 0;
  numbermatrix[7][5] = 0;
  numbermatrix[7][6] = 0;
  numbermatrix[8][0] = 240;
  numbermatrix[8][1] = 242;
  numbermatrix[8][2] = 244;
  numbermatrix[8][3] = 246;
  numbermatrix[8][4] = 248;
  numbermatrix[8][5] = 250;
  numbermatrix[8][6] = 252;
  numbermatrix[9][0] = 252;
  numbermatrix[9][1] = 250;
  numbermatrix[9][2] = 240;
  numbermatrix[9][3] = 242;
  numbermatrix[9][4] = 244;
  numbermatrix[9][5] = 246;
  numbermatrix[9][6] = 0;

  uint8_t checkdecomposed;
  uint8_t runmatrix;
  uint8_t multwo = 1;
  uint8_t pos240 = 0;
  uint8_t pos242 = 0;
  uint8_t pos244 = 0;
  uint8_t pos246 = 0;
  uint8_t pos248 = 0;
  uint8_t pos250 = 0;
  uint8_t pos252 = 0;
  uint8_t digi = 0;

  for (runmatrix = 0; runmatrix < 8; runmatrix++) {
    digi = numdecomposed[runmatrix];
    for (checkdecomposed = 0; checkdecomposed < 7; checkdecomposed++) {
      if (numbermatrix[digi][checkdecomposed] == 240) {
        pos240 = pos240 + multwo;
      } else if (numbermatrix[digi][checkdecomposed] == 242) {
        pos242 = pos242 + multwo;
      } else if (numbermatrix[digi][checkdecomposed] == 244) {
        pos244 = pos244 + multwo;
      } else if (numbermatrix[digi][checkdecomposed] == 246) {
        pos246 = pos246 + multwo;
      } else if (numbermatrix[digi][checkdecomposed] == 248) {
        pos248 = pos248 + multwo;
      } else if (numbermatrix[digi][checkdecomposed] == 250) {
        pos250 = pos250 + multwo;
      } else if (numbermatrix[digi][checkdecomposed] == 252) {
        pos252 = pos252 + multwo;
      }
    }
    multwo = multwo * 2;
  }



  digitalWrite(strobe, LOW); //set the strobe low so it'll accept instruction
  shiftOut(data, clock, LSBFIRST, 0x40); //set to single address mode
  digitalWrite(strobe, HIGH); //the mode is now set, we must set the strobe back to high
  digitalWrite(strobe, LOW);

  shiftOut(data, clock, LSBFIRST, 240);
  shiftOut(data, clock, LSBFIRST, pos240);
  shiftOut(data, clock, LSBFIRST, 242);
  shiftOut(data, clock, LSBFIRST, pos242);
  shiftOut(data, clock, LSBFIRST, 244);
  shiftOut(data, clock, LSBFIRST, pos244);
  shiftOut(data, clock, LSBFIRST, 246);
  shiftOut(data, clock, LSBFIRST, pos246);
  shiftOut(data, clock, LSBFIRST, 248);
  shiftOut(data, clock, LSBFIRST, pos248);
  shiftOut(data, clock, LSBFIRST, 250);
  shiftOut(data, clock, LSBFIRST, pos250);
  shiftOut(data, clock, LSBFIRST, 252);
  shiftOut(data, clock, LSBFIRST, pos252);
  if (numdecomposed[9] == 1) {
    shiftOut(data, clock, LSBFIRST, 238);
    shiftOut(data, clock, LSBFIRST, 255);
  }
  digitalWrite(strobe, HIGH);

  for (nuke = 1; nuke < 10; nuke++) {
    numdecomposed[nuke] = 0;
  }
  pos240 = 0;
  pos242 = 0;
  pos244 = 0;
  pos246 = 0;
  pos248 = 0;
  pos250 = 0;
  pos252 = 0;

}

uint8_t getkeypressed() {
  uint8_t keypressed = 0;
  uint8_t kbyt1 = 0;
  uint8_t kbyt2 = 0;
  uint8_t kbyt3 = 0;
  uint8_t kbyt4 = 0;
  digitalWrite(strobe, LOW);
  shiftOut(data, clock, LSBFIRST, 0x42);
  pinMode(data, INPUT_PULLUP);
  kbyt1 = shiftIn(data, clock, LSBFIRST);
  kbyt2 = shiftIn(data, clock, LSBFIRST);
  kbyt3 = shiftIn(data, clock, LSBFIRST);
  kbyt4 = shiftIn(data, clock, LSBFIRST);
  digitalWrite(strobe, HIGH);
  pinMode(data, OUTPUT);
  delay(10);

  if (kbyt1 == 4) {
    keypressed = 1;
  } else if (kbyt1 == 64) {
    keypressed = 2;
  } else if (kbyt2 == 4) {
    keypressed = 3;
  } else if (kbyt2 == 64) {
    keypressed = 4;
  } else if (kbyt3 == 4) {
    keypressed = 5;
  } else if (kbyt3 == 64) {
    keypressed = 6;
  } else if (kbyt4 == 4) {
    keypressed = 7;
  } else if (kbyt4 == 64) {
    keypressed = 8;
  } else if (kbyt1 == 2) {
    keypressed = 9;
  } else if (kbyt1 == 32) {
    keypressed = 10;
  } else if (kbyt2 == 2) {
    keypressed = 11;
  } else if (kbyt2 == 32) {
    keypressed = 12;
  } else if (kbyt3 == 2) {
    keypressed = 13;
  } else if (kbyt3 == 32) {
    keypressed = 14;
  } else if (kbyt4 == 2) {
    keypressed = 15;
  } else if (kbyt4 == 32) {
    keypressed = 16;
  } else {
    keypressed = 0;
  }
  return keypressed;
}

/* needed for I/O */
long p;

long readint() {
  p = 0;
  word button = 0;
  tmreset();
  while (button != 13) {
    button = 0;
    while (button == 0) {button = getkeypressed(); delay(150);}

    if ((button > 0) && (button < 11)) {
      p = p * 10 + (button - 1); // start with 0, not 1

    } else if (button == 11) {
      p = -p;

    } else if (button == 12) { 
      p = 0;
    }
    displaynumber(p);
  }

  return p;
}



void setup() {

/* set the delay */
datum[SPEEDVAR] = SPEEDDELAY; // delay between instructions on execution, user-settable

pinMode(strobe, OUTPUT);
pinMode(clock, OUTPUT);
pinMode(data, OUTPUT);

digitalWrite(strobe, LOW); //set the strobe low so it'll accept instruction
shiftOut(data, clock, LSBFIRST, 0x8F);  // send the instruction to activate the board and set brightness to max
digitalWrite(strobe, HIGH); //we will always set the strobe high after the completion of each instruction and data set
tmreset();

while (1) {
/* the highest level general loop which oscillates between command mode */
/* and general run mode */

/* ------------------------ SETUP AND MAINTENANCE ------------------------ */
/* Read in instruction orders: */
/* - positive addresses: program for later execution; */
/* - zero-command-address: immediate execution according to operators; */
/* - specific negative addresses: specific immediate actions, not covered */
/*   by the usual operators. */
/* This is a kind of REPL or user command interface prior to actually */
/* "running" any sort of longer program. Here, the user still has more */
/* immediate influence over the machine. */

runlimit = 3600; 

while (1) {
pc = 0; /* safety: nuke the program counter and the immediate instruction */


/* Arduino: flush here any read-cache before reading instructions. */

/* First, get the command address - to determine is it +, 0 or - .*/
/* From there it will depend what action is to be undertaken. */
displaynumber(-88888888); delay(1000);
cmdadr = readint();
/* atof and atoi due to lack of scanf */

/* COMMAND SELECTION IN REPL MODE */
/* Positive or zero - give the details of the instruction to be executed. */
if (cmdadr >= 0) {
  /* GIVE INSTRUCTION */

  opr = readint();
  
  datadr1 = readint();
  
  datadr2 = readint();
  
  datadr3 = readint();

  /* Having read the instruction, compose it and save it. */
  
  ww = opr & 31;
  xx = datadr1 & 511;
  yy = datadr2 & 511;
  zz = datadr3 & 511;
  instr = (ww << 27) | (xx << 18) | (yy << 9) | zz;
  
  EEPROM.put((INSTRUCTION_BYTE_LENGTH * cmdadr) , instr);
  // delay(10);

if (cmdadr == 0) {
    break; /* Go execute the command and return, unless it is a jump: */
           /* a jump does not necessarily return, unless the execution */
           /* either reaches the end of the instruction space - */
           /* which it totally may without encountering any error, */
           /* given that 0 is "no operation" and the memory is filled */
           /* with it at the beginning - or the execution encounters */
           /* a jump back to command address 0. Such jumps may indeed */
           /* be used to "partition" the execution space into */
           /* "different programs" if desired, which are separated from */
           /* each other by "forced returns". In the immediate interpreter, */
           /* the user may then decide which "entry point" to jump to */
           /* in order to trigger execution at the desired section. */
  }
}  else if (cmdadr == -1) {
  EEPROM.get((INSTRUCTION_BYTE_LENGTH * readint()) , instr);

  /* print AABBCCDD, where AA is the operation, and BB, CC and DD the data: */

  displaynumber(((instr >> 27) & 31) * 1000000 + ((instr >> 18) & 511) * 10000 + ((instr >> 9) & 511) * 100 + (instr & 511));
  delay(5000);
  
}  else if (cmdadr == -2) {
  /* LIST DATA - But now single datum, not range */

  i = readint();
  displaynumber(datum[i]); delay(5000);


} else if (cmdadr == -4) {
  /* ENTER DATA AS FLOATS WITHIN A PRE-SPECIFIED RANGE */
  /* Same as above, just this time, you enter data. */
  /* What is different: obviously, negative numbers are totally allowed. */
  /* So this time, the range is not flexible, but determined by two limits. */

  datum[readint()] = readint();

} else if (cmdadr == -5) {
  /* CLEAR INSTRUCTION RANGE */
  /* Simply erase to zero an entire section of program space, i.e. to */
  /* "no operation". */

  instr = 0; /* Just to make sure it "understands" the size as 32bit. */
  for (i = 0; i < EXECMEM; i++) {
    EEPROM.put((INSTRUCTION_BYTE_LENGTH * i) , instr);
  }

} else if (cmdadr == -6) {
  /* CLEAR DATA RANGE */
  /* Same idea as above, just clear data (set to 0), not instructions. */

  for (i = 0; i < DATAMEM; i++) {
    datum[i] = 0;
  }

} else if (cmdadr == -9) {
  runlimit = readint();
} /* end of command selection */

} /* end of setup and maintenance */










/* ----------------------- COMMAND EXECUTION PHASE ----------------------- */
/* All sails are set and you are now in the hands of Fate. The REPL has */
/* been exited, and whatever the Program Counter pc encounters is what */
/* shall be executed. If you enter into an infinite loop - tough luck! */
/* Actually, due to runlimit it should be not so bad now, either... */

/* As described, all commands are defined by an opcode operating on two */
/* addresses, whereby the first one also, generally, bears the result and */
/* is, roughly speaking, "more important". Often, the second address may be */
/* set to 0 as a short hand to signify that it does not matter. */


while (pc < EXECMEM) {
  if (runlimit == 1) {
    break; /* ejection if the runlimit is over, pc printed for re-entry */
  } else if (runlimit > 1) {
    runlimit--;
  } /* and do nothing if runlimit is 0. */

  /* Arduino: some sort of interruption due to reading the serial port */
  /* here would be desirable, for instance when reading "***".*/
  /* Either *** or ### are desirable, as they are similar to +++ */
  /* in modems, and moreover, are likely available on a mobile phone - */
  /* should this system ever be ported to a Java midlet. */
  /* .... Not necessary any more, since I implemented the run limit. */

  /* safety: erase the reserved datum[0]. */
  /* Do NOT erase here instruction[0], or you will prevent all execution: */
  /* whatever was there to be executed, would be annihilated! */
  datum[0] = 0; /* Useful for unconditional jumps. */

  EEPROM.get((INSTRUCTION_BYTE_LENGTH * pc) , instr);
  delay(10); /* For reliability. Yes, EEPROM can suffer such problems. */

  /* 63 operations possible, on 8191 places of data for each address; 26,13 */
  /* or: 255 operations, on 4095 data places for each address; 24,12 */
  /* Yeah, but here: 16 operations on 63 addresses. */

  ww = (instr >> 27) & 31;
  xx = (instr >> 18) & 511;
  yy = (instr >> 9) & 511;
  zz = instr & 511;

  opr = ww;
  datadr1 = xx;
  datadr2 = yy;
  datadr3 = zz;
 
  /* Is an instruction modifier active? If yes, change each dataddr. */
  /* This is due to IADR, indirect addressing, see below opcode 2. */
  if ((h > 0) || (k > 0)  || (l > 0)) {
    /* DEBUG */ /*
    Serial.print("executing IADR: ");
    Serial.print(datadr1);
    Serial.print("-->");
    Serial.print("h=");
    Serial.print(h);
    Serial.print("-->");
    Serial.print(datum[h]);
    Serial.print(" ");
    Serial.print(datadr2);
    Serial.print("-->");
    Serial.print("k=");
    Serial.print(k);
    Serial.print("-->");
    Serial.print(datum[k]);
    Serial.println();
    /* */

    datadr1 = h;
    datadr2 = k;
    datadr3 = l;
    h = 0;
    k = 0;
    l = 0;
  }

  /* DEBUG */ /*
  SRL.print("h=");
  SRL.print(h);
  SRL.print(" k=");
  SRL.print(k);
  SRL.print(" opr=");
  SRL.print(opr);
  SRL.print(" d1=");
  SRL.print(datadr1);
  SRL.print(" d2=");
  SRL.print(datadr2);
  SRL.print(" pc=");
  SRL.print(pc);
  SRL.println(); /* */

  /* ALWAYS trace execution. */
  displaynumber(pc * 100 + opr); delay(datum[SPEEDVAR]);
  
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

/* DEBUG */
/*   SRL.println("Entering decoder."); */

  if (opr == 0) {
  /* NOOP -- NO OPERATION: Do nothing. */
  /* Any non-implemented opcode will work as NOP. */
  /* This is also nice if you want to temporarily de-activate certain code. */

  } else if (opr == 1) {
  /* JUMP */
  /* JUMP IF THE FIRST ADR CONTAINS (NEARLY) ZERO OR A NEGATIVE NUMBER*/
  /* "Nearly" zero, because these are floats and repeated float operations */
  /* may render the results imprecise. */
  /* This can very neatly be translated into a high language's IF & WHILE. */
  /* Anyway, welcome to "spaghetti programming". You will use this sole */
  /* condition until you learn to LOVE IT! */
  
    k = 0;

    
    /* if the third address is above 6, then jump to the second half of the */
    /* address space */
    if (datadr3 > 13) {
      datadr3 = datadr3 - 14;
      k = 64;
    }


    if (((datum[datadr1] == 0) && (datadr3 == 7)) ||
        ((datum[datadr1] > 0) && (datadr3 == 8)) ||
        ((datum[datadr1] < 0) && (datadr3 == 9)) ||
        ((datum[datadr1] >= 0) && (datadr3 == 10)) ||
        ((datum[datadr1] <= 0) && (datadr3 == 11)) ||
        ((datum[datadr1] != 0) && (datadr3 == 12)) ||
        (datadr3 == 13)) {

      pc = datum[datadr2] + k; 
      k = 0;
    
    /* Now, have a whole potpourri of possible jumps. */ 
    } else if (((datum[datadr1] == 0) && (datadr3 == 0)) ||
        ((datum[datadr1] > 0) && (datadr3 == 1)) ||
        ((datum[datadr1] < 0) && (datadr3 == 2)) ||
        ((datum[datadr1] >= 0) && (datadr3 == 3)) ||
        ((datum[datadr1] <= 0) && (datadr3 == 4)) ||
        ((datum[datadr1] != 0) && (datadr3 == 5)) ||
        (datadr3 == 6)) { // 6: unconditional jump

      pc = datadr2 + k; 
      k = 0;
      
      /* On purpose NOT "computed", but IADR still allows it. */
      /* In fact, on ATTINY85 you NEED IADR in order to make jumps */
      /* beyond instruction 63, because lo and behold, the way jump is */
      /* implemented, datadr2 CANNOT JUMP BEYOND! For IADR, set the */
      /* destination times 100, i.e. to jump to position 80, jump to 8000. */
      /* The obvious "deal with the devil" would be to make the jumps */
      /* computed by default, but that would be incompatible to any other */
      /* 1V0 virtual machine. Unfortunately, both computed jumps and IADR */
      /* mean a mixing of code and data - where code is responsible for */
      /* jumps - and I regard this as sufficiently dangerous to attempt to */
      /* abstain. - Another way to do it - however, incompatible - would */
      /* be to multiply datadr2 * 4 to give you the jump address. Thus, */
      /* You would "decrease jump resolution", but could still go */
      /* anywhere, 63*4 = 252, so you could totally do this, and a jump */
      /* "resolution" of 1, given that you have NOOP, is in no way */
      /* "strictly necessary". However, the incompatibility was not deemed */
      /* acceptable. */
      
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

    /* Here, I DID accept an incompatibility: in reality, the code */
    /* here should be divided by 100, to make up for the fact that */
    /* ALL the numbers here are divisible by 100 due to their */
    /* fixed point. Now, the trouble with this was, in practice it */
    /* was simply too error prone. And IN THE END, in jumps, it was */
    /* STILL "incompatible", because the address finally indicated */
    /* by k was read by the jump as ABSOLUTE. (Had I made jumps */
    /* computed, this would not be an issue, but here I go.) */
    /* So now to have "the one thing divisible by 100, but the other */
    /* one not" was incredibly error-prone. So better than puzzling */
    /* the user with a myriad adjustments, the rule is simple: */
    /* despite all number nature, addresses are always absolute */
    /* integers and they are NOT adjusted. - On the bright side: */
    /* while computations with addresses ARE POSSIBLE, they are */
    /* just a little discouraged, and they CAN be adjusted, */
    /* however, what is more: addresses within the data block */
    /* will now immediately jump into view, as nearly all other */
    /* numbers will normally be bigger than them. After all, who */
    /* will use a system with two decimal digits precision to do */
    /* very much maths below 1... */

    h = datum[datadr1];
    k = datum[datadr2];
    l = datum[datadr3];
    /* Thereby, h & k are "charged" and will trigger a modification of */
    /* the next instruction, substituting its addresses one time. */
    /* You can obviously cancel this with a NOP. */

    /* DEBUG */ /*
    Serial.print("setting IADR: ");
    Serial.print(datadr1);
    Serial.print("-->");
    Serial.print("h=");
    Serial.print(h);
    Serial.print("-->");
    Serial.print(datum[h]);
    Serial.print(" ");
    Serial.print(datadr2);
    Serial.print("-->");
    Serial.print("k=");
    Serial.print(k);
    Serial.print("-->");
    Serial.print(datum[k]);
    Serial.println();
   /* */

  /* ----------- FROM NOW ON FOLLOW COMMANDS FOR SETTING VALUES ----------- */

  } else if (opr == 5) {
  /* SADR -- SET ADDRESS AS VALUE: */
  /* This can also used to let an address be treated as a value in some */
  /* future instruction - IADR (opcode 2) can help reversing the process. */
    /* DEBUG */ /*
    SRL.println("Entered SADR");
    SRL.flush(); */


    datum[datadr3] = (datadr1*100) + datadr2; 

  } else if (opr == 6) {
  /* SVAL -- SET VALUE: This is the "normal 'equal'" in other languages. */
  /* Did not call it EQUL or thelike, to help visually differentiate */
  /* values and addresses. */

    datum[datadr3] = datum[datadr2];
    
  } else if (opr == 8) {
  /* IVAS -- indirectly addressed assignment. */

    datum[datum[datadr3]] = datum[datadr2];

  /* ---- FROM NOW ON FOLLOW COMMANDS FOR SINGLE NUMERIC MANIPULATION ----- */
  } else if (opr == 9) {
  /* PLUS -- PLUS FOR SINGLE NUMBERS, NOT RANGE. */

    datum[datadr3] = datum[datadr1] + datum[datadr2];

  } else if (opr == 10) {
  /* MINS -- MINUS FOR SINGLE NUMBERS, NOT RANGE. */

    datum[datadr3] = datum[datadr1] - datum[datadr2];

  } else if (opr == 11) {
  /* MULS -- MULTIPLY SINGLE NUMBERS, NOT RANGE. */

    datum[datadr3] = datum[datadr1] * datum[datadr2] / 100;

  } else if (opr == 12) {
  /* DIVS -- DIVIDE SINGLE NUMBERS, NOT RANGE. */
  /* In case of a division by 0, there shall be no hysteria. Just give 0. */
  /* This makes loops much easier than "having to look out for the */
  /* mathematical booby-trap" all the time. Here this is done GENERALLY. */

    if (datum[datadr2] != 0) {
      datum[datadr3] = datum[datadr1] * 100 / datum[datadr2];
    } else {
      datum[datadr3] = 0;
    }

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

} /* end execution phase */

} /* end highest level general loop */

} /* end void-setup of Arduino */

void loop() {}
