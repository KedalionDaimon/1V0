#include <stdio.h>

/* Tzvet1V0 1V0 TZ III */

/* These DATAMEM and EXECMEM hold data and instructions, respectively. */
#define DATAMEM 452

/* But the above REQUIRES IADR jumps above 63, a frequent source of errors. */
/* You can activate it freely without any penalty, except said errors! */
/* If you want to jump anywhere without having to use IADR: */
#define EXECMEM 255

long datum[DATAMEM];
int instruction[EXECMEM*4];

/* instruction format: */
/* operator, data address 1 (first operand), data address 2 (second operand)*/
/* result address (third operand)*/
/* every operator is at a command address. */
/* so, basically, something like 4, 9, 10, 20, 21 will mean */
/* as a fourth programming step (4), add (9) the numbers located at */
/* data addresses 10 and 20 into a result and store that result at */
/* address 21 */
char cmdadr = 0;
short int opr = 0;
short int datadr1 = 0;
short int datadr2 = 0;
short int datadr3 = 0;

short int pc = 0;
short int h = 0; /* beware: h & k are used for "transcendental" instructions ... */
short int i = 0;     /* i & j are auxiliary looping variables */
short int j = 0;
short int k = 0; /* ... so ZERO THEM OUT after non-transcendental usage */
short int l = 0;
long p = 0;

unsigned int runlimit = 0;
/* If this is 0, run unlimited, else break when it becomes 1. */
/* This is a sort of "safety", to prevent infinite looping. */


void displaynumber(long nmbr) {
  printf("%ld", nmbr);
  printf("\n");
}

long readint() {
  scanf("%ld", &p);
  return p;
}



int main(void) {


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
displaynumber(-88888888);
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
  
  instruction[cmdadr * 4] = opr;
  instruction[(cmdadr * 4) + 1] = datadr1;
  instruction[(cmdadr * 4) + 2] = datadr2;
  instruction[(cmdadr * 4) + 3] = datadr3;

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
  i = readint();
  displaynumber(instruction[i * 4]);
  displaynumber(instruction[(i * 4) + 1]);
  displaynumber(instruction[(i * 4) + 2]);
  displaynumber(instruction[(i * 4) + 3]);

}  else if (cmdadr == -2) {
  /* LIST DATA - But now single datum, not range */

  i = readint();
  displaynumber(datum[i]);


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

  for (i = 0; i < EXECMEM; i++) {
    instruction[i] = 0;
  }

} else if (cmdadr == -6) {
  /* CLEAR DATA RANGE */
  /* Same idea as above, just clear data (set to 0), not instructions. */

  for (i = datadr1; i < DATAMEM; i++) {
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

  /* safety: erase the reserved datum[0]. */
  /* Do NOT erase here instruction[0], or you will prevent all execution: */
  /* whatever was there to be executed, would be annihilated! */
  datum[0] = 0; /* Useful for unconditional jumps. */

  opr = instruction[pc * 4];;
  datadr1 = instruction[(pc * 4) + 1];
  datadr2 = instruction[(pc * 4) + 2];
  datadr3 = instruction[(pc * 4) + 3];
 
  /* Is an instruction modifier active? If yes, change each dataddr. */
  /* This is due to IADR, indirect addressing, see below opcode 2. */
  if ((h > 0) || (k > 0)  || (l > 0)) {

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
  displaynumber(pc * 100 + opr);
  
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
    if (datadr3 > 6) {
      datadr3 = datadr3 - 7;
      k = 64;
    }
    
    /* Now, have a whole potpourri of possible jumps. */ 
    if (((datum[datadr1] == 0) && (datadr3 == 0)) ||
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
  /* SUBSTITUTE datadr1 AND datadr2 AND datadr3 IN THE NEXT INSTRUCTION */
  /* BY h AND k AND l. */
  /* Whatever the next operation is, datum[datadr1] shall give */
  /* the next datadr1, and datum[datadr2] shall give the next datadr2, */
  /* and datum[datadr3] shall give datadr3. */
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



  /* ----------- FROM NOW ON FOLLOW COMMANDS FOR SETTING VALUES ----------- */

  } else if (opr == 5) {
  /* SADR -- SET ADDRESS AS VALUE: */
  /* This can also used to let an address be treated as a value in some */
  /* future instruction - IADR (opcode 2) can help reversing the process. */

    datum[datadr3] = datadr2 + datadr1; /* should have been times 100, but this is */
                              /* too much fuss. */
    /* obviously, if datadr2 == datadr1, this will make an address */
    /* "hold itself" */

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
    /* thus freely selectable. */
      break;
    }
  }

} /* end execution phase */

} /* end highest level general loop */

} /* end void-setup of Arduino */


