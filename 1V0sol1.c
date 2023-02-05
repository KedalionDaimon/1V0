/*
#include "lib/stdio.asm"

int readint() {
  int n;
  asm{
       call scan_u16d
       mov ^n, a
  }
  return n;
}


void displaynumber(int nmbr) {
  asm{
    mov a, ^n
    swp a ; swap to make up for stack weirdness
    call print_u16d

    mov a, 0
    mov ah, $0A
    call putchar
  }
  return;
}
*/


/*
#include <stdio.h>

void displaynumber(int nmbr) {
  printf("%d", nmbr);
  printf("\n");
}

int readint() {
  scanf("%d", &p);
  return p;
}

INSTEAD-SEE-DUMMY-FUNCTIONS-BELOW

*/

int displaynumber() {

}

int readint() {
  return 10;
}



int datum[1023];

int instruction[1020];

int cmdadr = 0;
int opr = 0;
int datadr1 = 0;
int datadr2 = 0;
int datadr3 = 0;

int pc = 0;
int h = 0;
int i = 0;
int j = 0;
int k = 0;
int l = 0;
int p = 0;

int runlimit = 0;

int main(void) {

while (1) {

runlimit = 3600; 

while (1) {
pc = 0; 
displaynumber(12312);
cmdadr = readint();

if (cmdadr <= 255) {

  opr = readint();
  
  datadr1 = readint();
  
  datadr2 = readint();
  
  datadr3 = readint();

  instruction[cmdadr * 4] = opr;
  instruction[(cmdadr * 4) + 1] = datadr1;
  instruction[(cmdadr * 4) + 2] = datadr2;
  instruction[(cmdadr * 4) + 3] = datadr3;

if (cmdadr == 0) {
    break; 
  }
}  else if (cmdadr == 1111) {
  i = readint();
  displaynumber(instruction[i * 4]);
  displaynumber(instruction[(i * 4) + 1]);
  displaynumber(instruction[(i * 4) + 2]);
  displaynumber(instruction[(i * 4) + 3]);

}  else if (cmdadr == 2222) {
  i = readint();
  displaynumber(datum[i]);


} else if (cmdadr == 4444) {

  datum[readint()] = readint();

} else if (cmdadr == 5555) {

  for (i = 0; i < 255; i++) {
    instruction[i] = 0;
  }

} else if (cmdadr == 6666) {

  for (i = datadr1; i < 1023; i++) {
    datum[i] = 0;
  }

} else if (cmdadr == 9999) {
  runlimit = readint();
}

}



while (pc < 255) {
  if (runlimit == 1) {
    break;
  } else if (runlimit > 1) {
    runlimit--;
  }


  datum[0] = 0;

  opr = instruction[pc * 4];
  datadr1 = instruction[(pc * 4) + 1];
  datadr2 = instruction[(pc * 4) + 2];
  datadr3 = instruction[(pc * 4) + 3];

  if ((h > 0) || (k > 0)  || (l > 0)) {

    datadr1 = h;
    datadr2 = k;
    datadr3 = l;
    h = 0;
    k = 0;
    l = 0;
  }

  displaynumber(pc * 100 + opr);
  
  if (opr == 0) {

  } else if (opr == 1) {

    k = 0;

    if (datadr3 > 6) {
      datadr3 = datadr3 - 7;
      k = 64;
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

    datum[datum[datadr3]] = datum[datadr2];

  } else if (opr == 9) {

    datum[datadr3] = datum[datadr1] + datum[datadr2];

  } else if (opr == 10) {

    datum[datadr3] = datum[datadr1] - datum[datadr2];

  } else if (opr == 11) {

    datum[datadr3] = datum[datadr1] * datum[datadr2] / 100;

  } else if (opr == 12) {

    if (datum[datadr2] != 0) {
      datum[datadr3] = datum[datadr1] * 100 / datum[datadr2];
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

}


