#define DATAMEM 2001
#define EXECMEM 1001

#include <stdio.h>

long long datum[DATAMEM];
unsigned int instruction[EXECMEM];

long instr = 0;

int cmdadr = 0;
int opr = 0;
int datadr1 = 0;
int datadr2 = 0;
int datadr3 = 0;
unsigned long ww = 0;
unsigned long xx = 0;
unsigned long yy = 0;
unsigned long zz = 0;
short tabula = 0;

int pc = 0;
int h = 0;
int i = 0;
int j = 0;
int k = 0;
int l = 0;
long long f = 0;

unsigned int runlimit = 0;

main() {

for (i=0; i<EXECMEM; i++) {
  instruction[i] = 0;
}

for (i=0; i<DATAMEM; i++) {
  datum[i] = 0;
}

while (1) {



runlimit = 5 * EXECMEM; 

while (1) {
pc = 0; 

printf("\nCMD ADDRESS: ");
scanf("%d", &cmdadr);

if (cmdadr >= 0) {
  printf("OPERATION  : ");
  scanf("%d", &opr);
  printf("DATA ADDR A: ");
  scanf("%d", &datadr1);
  printf("DATA ADDR B: ");
  scanf("%d", &datadr2);
  printf("DATA ADDR C: ");
  scanf("%d", &datadr3);

  if ((datadr1 >= 0) && (datadr2 >= 0) && (datadr3 >= 0)) {

  ww = opr & 31;
  xx = datadr1 & 511;
  yy = datadr2 & 511;
  zz = datadr3 & 511;
  instr = (ww << 27) | (xx << 18) | (yy << 9) | zz;

  instruction[cmdadr] = instr; 
  }

if (cmdadr == 0) {
    break; 
  }
} else if (cmdadr == -1) {

  printf("LIST INSTRUCTION RANGE, INCLUSIVE LIMITS.\n");
  printf("FIRST COMMAND ADDRESS FROM WHERE ON TO LIST: ");
  scanf("%d", &datadr1);
  printf("SECOND COMMAND ADDRESS AND TO WHERE TO LIST: ");
  scanf("%d", &datadr2);

  if ((datadr1 < 0) || (datadr2 < 0)) { break; }

  for (i = datadr1; i <= datadr2; i++) {
    instr =  instruction[i];

    printf("%d", i);

    printf("%ld", -((long) ((instr >> 27) & 31)));

    printf("%ld", -((long) ((instr >> 18) & 511)));

    printf("%d", -((int) ((instr >> 9) & 511)));

    printf("%d ", -((int) (instr & 511)));

    tabula++;
    if (tabula == 4) { tabula = 0; printf("\n");}
  }
  printf("\n"); tabula = 0;

} else if (cmdadr == -2) {

  printf("LIST DATA RANGE, INCLUSIVE LIMITS.\n");
  printf("FIRST DATA ADDRESS FROM WHERE ON TO LIST: ");
  scanf("%d", &datadr1);
  printf("SECOND DATA ADDRESS AND TO WHERE TO LIST: ");
  scanf("%d", &datadr2);
  if ((datadr1 < 0) || (datadr2 < 0)) { break; }

  for (i = datadr1; i <= datadr2; i++) {
    printf("%lld", datum[i]);
    printf("%d ", -i);
    tabula++;
    if (tabula == 4) { tabula = 0; printf("\n");}
  }
  printf("\n"); tabula = 0;

} else if (cmdadr == -4) {

  printf("ENTER DATA WITHIN INCLUSIVE RANGE LIMITS.\n");
  printf("DATA ADDRESS FROM WHERE ON TO ENTER: ");
  scanf("%d", &datadr1);
  printf("DATA ADDRESS UNTIL WHERE TO ENTER  : ");
  scanf("%d", &datadr2);
  if ((datadr1 < 0) || (datadr2 < 0) || (datadr2 < datadr1)) { break; }

  while (datadr1 <= datadr2) {
    printf("%04d: DATUM: ", datadr1);
    scanf("%lld", &f);
    datum[datadr1] = f;
    datadr1++;
  }


} else if (cmdadr == -5) {

  printf("CLEAR INSTRUCTION RANGE, INCLUSIVE LIMITS.\n");
  printf("FIRST COMMAND ADDRESS FROM WHERE ON TO CREAR: ");
  scanf("%d", &datadr1);
  printf("SECOND COMMAND ADDRESS AND TO WHERE TO CLEAR: ");
  scanf("%d", &datadr2);
  if ((datadr1 < 0) || (datadr2 < 0)) { break; }

  for (i = datadr1; i <= datadr2; i++) {
    instruction[i] = 0;
  }
  printf("INSTRUCTIONS CLEARED.\n");
} else if (cmdadr == -6) {

  printf("CLEAR DATA RANGE, INCLUSIVE LIMITS.\n");
  printf("FIRST DATA ADDRESS FROM WHERE ON TO CLEAR: ");
  scanf("%d", &datadr1);
  printf("SECOND DATA ADDRESS AND TO WHERE TO CLEAR: ");
  scanf("%d", &datadr2);
  if ((datadr1 < 0) || (datadr2 < 0)) { break; }

  for (i = datadr1; i <= datadr2; i++) {
    datum[i] = 0;
  }
  printf("DATA CLEARED.\n");


} else if (cmdadr == -9) {
  printf("SET RUNLIMIT, UP TO 65535, 0=INFINITE, TERMINATING WHEN 1: ");
  scanf("%u", &runlimit);
} else if (cmdadr == -10) {
  printf("SYSTEM RUN TERMINATED\n");
  goto exxitt;
} 

}






while (pc < EXECMEM) {
  if (runlimit == 1) {
    printf("RUNLIMIT EXHAUSTED PC=%d RUN STOPPED\n", pc);
    break;
  } else if (runlimit > 1) {
    runlimit--;
    if (((runlimit - 1) % 200) == 0) {
      printf("RUNLIMIT = %u\n", runlimit);

    }
  }

  datum[0] = 0; 

  if (pc < 0) {
    pc = 0;
  }

  instr = instruction[pc];

  opr = (instr >> 27) & 31;
  datadr1 = (instr >> 18) & 511;
  datadr2 = (instr >> 9) & 511;
  datadr3 = instr & 511;

  ww = (instr >> 27) & 31;
  xx = (instr >> 18) & 511;
  yy = (instr >> 9) & 511;
  zz = instr & 511;

  opr = ww;
  datadr1 = xx;
  datadr2 = yy;
  datadr3 = zz;
 
  if ((h > 0) || (k > 0) || (l > 0)) {

    datadr1 = h;
    datadr2 = k;
    datadr3 = l;
    h = 0;
    k = 0;
    l = 0;
  }

  printf("%d%d%d%d%d ", pc, -opr, -datadr1, -datadr2, -datadr3);
  tabula++;
  if (tabula == 4) { tabula = 0; printf("\n");}

  if (opr == 0) {

  } else if (opr == 1) {
  
    k = 0;

    if (datadr3 > 6) {
      datadr3 = datadr3 - 7;
      k = 512;
    }
    
    if (((datum[datadr1] == 0) && (datadr3 == 0)) ||
        ((datum[datadr1] > 0) && (datadr3 == 1)) ||
        ((datum[datadr1] < 0) && (datadr3 == 2)) ||
        ((datum[datadr1] >= 0) && (datadr3 == 3)) ||
        ((datum[datadr1] <= 0) && (datadr3 == 4)) ||
        ((datum[datadr1] != 0) && (datadr3 == 5)) ||
        (datadr3 == 6)) {

      pc = datadr2 + k; 
      k = 0;

    } else { pc++; }

  } else if (opr == 2) {


    h = datum[datadr1];
    k = datum[datadr2];
    l = datum[datadr3];


  } else if (opr == 5) {


    datum[datadr3] = datadr2 + datadr1;

  } else if (opr == 6) {

    datum[datadr3] = datum[datadr2];
    
  } else if (opr == 8) {

    i = datum[datadr3];
    datum[i] = datum[datadr2];

  } else if (opr == 9) {

    datum[datadr3] = datum[datadr1] + datum[datadr2];

  } else if (opr == 10) {

    datum[datadr3] = datum[datadr1] - datum[datadr2];

  } else if (opr == 11) {
 
    datum[datadr3] = datum[datadr1] * datum[datadr2] / 10000;

  } else if (opr == 12) {

    if (datum[datadr2] != 0) {
      datum[datadr3] = datum[datadr1] * 10000 / datum[datadr2];
    } else {
      datum[datadr3] = 0;
    }

  }
 
  if (opr != 1) {
    if (pc > 0) {
      pc++;
    } else if (pc == 0) {
      break;
    }
  }

}

}

exxitt:

i = 0;

}

