/*
1 = +mantissa 2 = +exponent 3 = result
POXY 13

1 = number 2 = after decimal 3 = whole part
IFRA 15

1 = period 2 = +-percentage as decimal 3 = (1+percentage)^period
AMNT

SWAP 20

1 = rangebegin 2 = rangeend 3 = targetbegin
COPY 22

1 = rangebegin 2 = rangeend 3 = result
SUMR 28
SUSQ 29

1 = rangebegin 2 = rangeend 3 = targetbegin
CORS 26
IXTH 30
SQRT 32
ABSR 31
SQUA 33
CBRT 34
CUBE 35
SIND 40
COSD 41
TAND 42

1 = rangebegin 2 = rangeend 3 = mean, 3++ stdev
MSTD 46

1 = rangebegin 2 = rangeend 3 = ignored, result in range
SORT 25
TURN 27

1 = rangebegin 2 = rangeend 3 = begin of second range, result in first range
PLUR 53
MINR 54
MULR 55
DIVR 56

1 = rangebegin 2 = rangeend 3 = num, result in range
FRIS 23
PLUN 57
MINN 58
MULN 59
DIVN 60

cube root:
def newton(n):
  x = n
  i = 0
  while i < 10:
    x=x-(x**3-n)/(3.0*(x**2))
    i = i + 1
  return x

*/

#include <stdio.h>
#include <stdint.h>

uint16_t datum[2640];

uint16_t instruction[880];

uint16_t cmdadr = 0;
uint16_t opr = 0;
uint16_t datadr1 = 0;
uint16_t datadr2 = 0;
uint16_t datadr3 = 0;

uint16_t pc = 0;
uint16_t h = 0;
uint16_t i = 0;
uint16_t j = 0;
uint16_t k = 0;
uint16_t l = 0;
uint16_t p = 0;

uint16_t runlimit = 0;

uint16_t exitflag = 0;
uint16_t exitexec = 0;

uint16_t ionum[6] = {0,0,0,0,0,0};
uint16_t ionr = 0;
uint16_t ioshift = 0;

uint16_t datumpos = 0;

uint16_t anarr[12] = {0,0,0,0,0,0,0,0,0,0,0,0};
uint16_t bnarr[12] = {0,0,0,0,0,0,0,0,0,0,0,0};
uint16_t anarrbkp[12] = {0,0,0,0,0,0,0,0,0,0,0,0};
uint16_t bnarrbkp[12] = {0,0,0,0,0,0,0,0,0,0,0,0};

uint16_t cnarr[12] = {0,0,0,0,0,0,0,0,0,0,0,0};
uint16_t mulres[24] = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
uint16_t divres[12] = {0,0,0,0,0,0,0,0,0,0,0,0};
uint16_t asign = 0;
uint16_t bsign = 0;
uint16_t csign = 0;
uint16_t protopos = 0;
uint16_t pos = 0;
uint16_t carry = 0;
uint16_t nextcarry = 0;
uint16_t agtb = 0;
uint16_t bgta = 0;
uint16_t aeqb = 0;
uint16_t aneqb = 0;
uint16_t ageb = 0;
uint16_t bgea = 0;
uint16_t eqflag = 0;
uint16_t sizepos = 0;
uint16_t psizepos = 0;
uint16_t swappos = 0;
uint16_t swaptmp = 0;

uint16_t toolarge = 0;
uint16_t normal = 0;
uint16_t mulpos1 = 0;
uint16_t mulpos2 = 0;
uint16_t mulpos3 = 0;

uint16_t brshift = 0;
uint16_t blshift = 0;
uint16_t alshift = 0;
uint16_t divcounter1 = 0;
uint16_t divcounter2 = 0;
uint16_t segmentcounter = 0;
uint16_t divi = 0;
uint16_t allzeroes = 0;
uint16_t bkpcsign = 0;
uint16_t divshift = 0;
uint16_t subi = 0;
uint16_t posflag = 0;

uint16_t prni = 0;
uint16_t dati = 0;
uint16_t posi = 0;

uint16_t guess[12] = {0,0,0,0,0,0,0,0,0,0,0,0};
uint16_t num[12] = {0,0,0,0,0,0,0,0,0,0,0,0};

uint16_t crvarv[12] = {0,0,0,0,0,0,0,0,0,0,0,0};
uint16_t crvarw[12] = {0,0,0,0,0,0,0,0,0,0,0,0};
uint16_t crvarx[12] = {0,0,0,0,0,0,0,0,0,0,0,0};
uint16_t crvary[12] = {0,0,0,0,0,0,0,0,0,0,0,0};
uint16_t crvarz[12] = {0,0,0,0,0,0,0,0,0,0,0,0};
uint16_t signbkp = 0;
uint16_t cubepos = 0;
uint16_t cubeloop = 0;

uint16_t biszero = 0;

uint16_t traceon = 0;

uint16_t readint () {
  uint16_t gcidx = 0;
  uint16_t chargot = 0;

  p = 0;
  while ((chargot != '.') && (chargot != '\n')) {
    chargot = getchar();
    if ((chargot == '#') || (chargot == 8) || (chargot == 127)) {
      p = p / 10;
      printf("\r\n    %hd", p);
    } else {
      if (chargot == '0') {
        chargot = 0;
      } else if (chargot == '1') {
        chargot = 1;     
      } else if (chargot == '2') {
        chargot = 2;     
      } else if (chargot == '3') {
        chargot = 3;     
      } else if (chargot == '4') {
        chargot = 4;     
      } else if (chargot == '5') {
        chargot = 5;     
      } else if (chargot == '6') {
        chargot = 6;     
      } else if (chargot == '7') {
        chargot = 7;     
      } else if (chargot == '8') {
        chargot = 8;     
      } else if (chargot == '9') {
        chargot = 9;     
      }
      if (chargot < 10) {
        p = (p * 10) + chargot;
      }
    }
 
  }

  return p;
}

void displaynumber (uint16_t nmbr) {
  printf ("%hd", nmbr);
  printf ("\n");
  return;
}

void prnnumspace(uint16_t n){
  printf ("%hd ", n);
  return;
}

void prnnum(uint16_t n){
  printf ("%hd", n);
  return;
}

void prnnl(){
  printf ("\n");
  return;
}

void prnsp(){
  printf (" ");
  return;
}

void frontzero(uint16_t n) {
  if (n < 1000) {
    prnnum(0);
  }
  if (n < 100) {
    prnnum(0);
  }
  if (n < 10) {
    prnnum(0);
  }
  prnnumspace(n);
  return;
}

void prndatum(uint16_t n) {
  for (prni = 6 + (n * 6); prni > (n * 6); prni--) {
    if (datum[prni-1] < 1000) {
      prnnum(0);
    }
    if (datum[prni-1] < 100) {
      prnnum(0);
    }
    if (datum[prni-1] < 10) {
      prnnum(0);
    }
    prnnum(datum[prni-1]);

    if (prni == 3 + (n * 6)) {
      prnsp();
    }

  }
  prnsp();
  return;
}

void chargea(uint16_t n) {
  for (pos = n * 6; pos < (n * 6) + 6; pos++) {
    ioshift = datum[pos]/100;
    anarr[(pos - (n * 6)) * 2 + 1] = ioshift;
    anarr[(pos - (n * 6))* 2 ] = datum[pos] - (ioshift * 100);
  }
  return;
}

void chargeb(uint16_t n) {
  for (pos = n * 6; pos < (n * 6) + 6; pos++) {
    ioshift = datum[pos]/100;
    bnarr[(pos - (n * 6)) * 2 + 1] = ioshift;
    bnarr[(pos - (n * 6))* 2 ] = datum[pos] - (ioshift * 100);
  }
  return;
}

void recordc(uint16_t n) {
  for (pos = n*6; pos < (n*6) + 6; pos++) {
    datum[pos] = (cnarr[(pos - (n*6))*2+1]) * 100 + cnarr[(pos - (n*6))*2];
  }
  return;
}

void fixsignin() {
  asign = 0;
  bsign = 0;
  csign = 0;
  if (anarr[11] > 9) {
    pos = anarr[11]/10;
    anarr[11] = anarr[11] - (pos * 10);
    asign = 1;
  }
  if (bnarr[11] > 9) {
    pos = bnarr[11]/10;
    bnarr[11] = bnarr[11] - (pos * 10);
    bsign = 1;
  }
  return;
}

void fixsignout() {
  if (csign == 1) {
    cnarr[11] = cnarr[11] + 10;
    csign = 0;
  }
  return;
}

void fixcsizezero() {
  allzeroes = 1;
  for (pos = 1; pos < 12; pos++) {
    if (cnarr[pos] != 0) {
      allzeroes = 0;
      break;
    }
  }
  if (allzeroes == 1) {
    csign = 0;
  }
  if (cnarr[11] > 9) {
    csign = 0;
    for (pos = 1; pos < 12; pos++) {
      cnarr[pos] = 0;
    }
  }
  return;
}

void swapab () {
  for (swappos = 0; swappos < 12; swappos++) {
    anarr[swappos] = anarr[swappos] + bnarr[swappos];
    bnarr[swappos] = anarr[swappos] - bnarr[swappos];
    anarr[swappos] = anarr[swappos] - bnarr[swappos];
  }
  return;
}

void checkabsabsize() {
  agtb = 0;
  bgta = 0;
  ageb = 0;
  bgea = 0;
  aeqb = 0;
  aneqb = 0;

  
  for (psizepos = 12; psizepos > 0; psizepos--) {
    sizepos = psizepos - 1;
    if (anarr[sizepos] > bnarr[sizepos]) {
      aneqb = 1;
      agtb = 1;
      ageb = 1;
    }
    if (anarr[sizepos] < bnarr[sizepos]) {
      aneqb = 1;
      bgta = 1;
      bgea = 1;
    }

    if (aneqb == 1) {
      break;
    }

  }
  if (aneqb == 0) {
    aeqb = 1;
    ageb = 1;
    bgea = 1;
  }
  return;
}

void protoplus() {
  carry = 0;
  for (pos = 0; pos < 12; pos++) {
    cnarr[pos] = anarr[pos] + bnarr[pos] + carry;
    carry = 0;
    if (cnarr[pos] > 99) {
      carry = 1;
      cnarr[pos] = cnarr[pos] - 100;
    }
  }
  return;
}

void protominus() {
  carry = 0;
  for (pos = 0; pos < 12; pos++) {
    nextcarry = 0;
    if ((bnarr[pos] + carry) > anarr[pos]) {
      anarr[pos] = anarr[pos] + 100;
      nextcarry = 1;
    }
    cnarr[pos] = anarr[pos] - bnarr[pos] - carry;
    carry = nextcarry;
  }
  if (carry == 1) {
    csign = 1;
    carry = 0;
  }
  return;
}

void pminus() {
  for (divi = 0; divi < 12; divi++) {
    cnarr[divi] = 0;
  }
  checkabsabsize();
  if ((asign == bsign) && (aeqb == 1)) {
    csign = 0;
    for (pos = 1; pos < 12; pos++) {
      cnarr[pos] = 0;
    }
  }

  if ((asign == 0) && (bsign == 0) && (ageb == 1)) {
    csign = 0;
    protominus();
  }

  if ((asign == 0) && (bsign == 0) && (bgta == 1)) {
    csign = 1;
    swapab();
    protominus();
  }

  if ((asign == 1) && (bsign == 1) && (ageb == 1)) {
    csign = 1;
    protominus();
  }

  if ((asign == 1) && (bsign == 1) && (bgta == 1)) {
    csign = 0;
    swapab();
    protominus();
  }

  if ((asign == 0) && (bsign == 1)) {
    csign = 0;
    protoplus();
  }

  if ((asign == 1) && (bsign == 0)) {
    csign = 1;
    protoplus();
  }

  return;
}

void minus() {
  fixsignin();
  pminus();
  fixcsizezero();
  fixsignout();
  return;
}

void pplus() {
  for (divi = 0; divi < 12; divi++) {
    cnarr[divi] = 0;
  }
  checkabsabsize();

  if (((asign == 0) && (bsign == 1)) && (aeqb == 1)) {
    asign = 0;
    bsign = 0;
    csign = 0;
    for (pos = 1; pos < 12; pos++) {
      cnarr[pos] = 0;
    }
    return;
  }

  if (((asign == 1) && (bsign == 0)) && (aeqb == 1)) {
    asign = 0;
    bsign = 0;
    csign = 0;
    for (pos = 1; pos < 12; pos++) {
      cnarr[pos] = 0;
    }
    return;
  }

  if ((asign == 0) && (bsign == 0)) {
    asign = 0;
    bsign = 0;
    csign = 0;
    protoplus();
    return;
  }

  if ((asign == 1) && (bsign == 1)) {
    asign = 0;
    bsign = 0;
    csign = 1;
    protoplus();
    return;
  }

  if ((asign == 0) && (bsign == 1) && (agtb == 1)) {
    asign = 0;
    bsign = 0;
    csign = 0;
    protominus();
    return;
  }

  if ((asign == 0) && (bsign == 1) && (bgta == 1)) {
    asign = 0;
    bsign = 0;
    csign = 1;
    swapab();
    protominus();
    return;
  }

  if ((asign == 1) && (bsign == 0) && (agtb == 1)) {
    csign = 1;
    swapab();
    asign = 0;
    bsign = 0;
    pminus();
    return;
  }

  if ((asign == 1) && (bsign == 0) && (bgta == 1)) {
    asign = 0;
    bsign = 0;
    csign = 0;
    swapab();
    protominus();
    return;
  }
  return;
}

void plus() {
  fixsignin();
  pplus();
  fixcsizezero();
  fixsignout();
  return;
}


void normmulres() {
  if (mulres[23] > 99) {
    toolarge = mulres[23]/100;
    mulres[23] = mulres[23] - (toolarge * 100);
  }

  normal = 0;
  while (normal == 0) {
    normal = 1;

    for (protopos = 0; protopos < 23; protopos++) {
      pos = 22 - protopos;

      if (mulres[pos] > 99) {
          normal = 0;
          toolarge = mulres[pos]/100;
          mulres[pos] = mulres[pos] - (toolarge * 100);
          mulres[pos + 1] = mulres[pos + 1] + toolarge;
      }
    }
  }

  if (mulres[23] > 99) {
    toolarge = mulres[23]/100;
    mulres[23] = mulres[23] - (toolarge * 100);
  }
  return;
}

void prototimes() {
  for (divi = 0; divi < 12; divi++) {
    cnarr[divi] = 0;
  }
  for (divi = 0; divi < 24; divi++) {
    mulres[divi] = 0;
  }
  for (mulpos1 = 0; mulpos1 < 12; mulpos1++) {
    for (mulpos2 = 0; mulpos2 < 12; mulpos2++) {
      mulres[mulpos1+mulpos2] =
      mulres[mulpos1+mulpos2] + (bnarr[mulpos2]*anarr[mulpos1]);
    }

    normmulres();
  }
  return;
}

void aincrease() {
  for (divi = 12; divi > 1; divi--) {
    subi = anarr[divi-1] / 10;
    subi = subi * 10;
    anarr[divi-1] = anarr[divi-1] - subi;
    anarr[divi-1] = anarr[divi-1] * 10;
    subi = anarr[divi-2] / 10;
    anarr[divi-1] = anarr[divi-1] + subi;
  }
  subi = anarr[0];
  anarr[0] = anarr[0] / 10;
  anarr[0] = anarr[0] * 10;
  anarr[0] = subi - anarr[0];
  anarr[0] = anarr[0] * 10;
  return;
}

void bincrease() {
  for (divi = 12; divi > 1; divi--) {
    subi = bnarr[divi-1] / 10;
    subi = subi * 10;
    bnarr[divi-1] = bnarr[divi-1] - subi;
    bnarr[divi-1] = bnarr[divi-1] * 10;
    subi = bnarr[divi-2] / 10;
    bnarr[divi-1] = bnarr[divi-1] + subi;
  }
  subi = bnarr[0];
  bnarr[0] = bnarr[0] / 10;
  bnarr[0] = bnarr[0] * 10;
  bnarr[0] = subi - bnarr[0];
  bnarr[0] = bnarr[0] * 10;
  return;
}

void adecrease() {
  for (divi = 0; divi < 11; divi++) {
    subi = anarr[divi+1]/10;
    subi = subi * 10;
    subi = anarr[divi+1] - subi;
    subi = subi * 10;
    anarr[divi] = subi + (anarr[divi]/10);
  }
  anarr[11] = anarr[11]/10;
  return;
}

void bdecrease() {
  for (divi = 0; divi < 11; divi++) {
    subi = bnarr[divi+1]/10;
    subi = subi * 10;
    subi = bnarr[divi+1] - subi;
    subi = subi * 10;
    bnarr[divi] = subi + (bnarr[divi]/10);
  }
  bnarr[11] = bnarr[11]/10;
  return;
}

void protodividedby() {
  posflag = 0;

  brshift = 0;
  blshift = 0;
  alshift = 0;
  divcounter1 = 0;
  divcounter2 = 0;
  segmentcounter = 0;

  allzeroes = 1;
  for (divi = 0; divi < 12; divi++) {
    cnarr[divi] = 0;
    divres[divi] = 0;
    if (bnarr[divi] != 0) {
      allzeroes = 0;
    }
  }
  if (allzeroes == 1) {
    return;
  }

  while (bnarr[11] == 0) {
    for (divi = 11; divi > 0; divi--) {
      bnarr[divi] = bnarr[divi-1];
    }
    bnarr[0] = 0;
    blshift = blshift + 2;
  }

  allzeroes = 1;
  for (divi = 0; divi < 12; divi++) {
    if (anarr[divi] != 0) {
      allzeroes = 0;
    }
  }
  if (allzeroes == 1) {
    return;
  }
  while (anarr[11] == 0) {
    for (divi = 0; divi < 11; divi++) {
      anarr[11-divi] = anarr[11-divi-1];
    }
    anarr[0] = 0;
    alshift = alshift + 2;
  }

  if (anarr[11] < 10) {
    alshift++;
    aincrease();
  }

  if (bnarr[11] >= 10) {
    brshift++;
    bdecrease();
  }

  segmentcounter = 0;
  divcounter1 = 0;
  divcounter2 = 0;

  posflag = 0;
  if (anarr[11] > bnarr[11]*10) {
    blshift++;
    adecrease();
  }

  while (segmentcounter < 12) {

    checkabsabsize();

    while (ageb == 1) {
      protominus();
      divcounter1++;
      for (divi = 0; divi < 12; divi++) {
        anarr[divi] = cnarr[divi];
        cnarr[divi] = 0;
      }
      checkabsabsize();
    }

    posflag++;

    if (posflag == 1) {
      divcounter2 = divcounter1 * 10;
      divcounter1 = 0;
    }

    aincrease();

    if (posflag == 2) {
      posflag = 0;

      divres[11-segmentcounter] = divcounter2 + divcounter1;
      divcounter1 = 0;
      divcounter2 = 0;
      segmentcounter++;   

      allzeroes = 1;
      for (divi = 0; divi < 12; divi++) {
        if (anarr[divi] != 0) {
          allzeroes = 0;
        }
      }
      if (allzeroes == 1) {
        return;
      }
    }

  }
  return;
}

void normdivres() {


  for (divi = 12; divi < 24; divi++) {
    mulres[divi] = 0;
  }
  for (divi = 0; divi < 12; divi++) {
    mulres[divi] = divres[divi];
  }

  normmulres();

  for (divshift = 0; divshift < 11; divshift++) {
    for (divi = 0; divi < 23; divi++) {
      mulres[23-divi] = mulres[23-divi-1];
    }
    mulres[0] = 0;
  }

  for (divi = 0; divi < 12; divi++) {
    divres[divi] = mulres[divi + 12];
  }

  return;
}

void times() {

  fixsignin();

  csign = 0;
  if (asign != bsign) {
    csign = 1;
  }
  asign = 0;
  bsign = 0;

  prototimes();

  allzeroes = 1;
  for (pos = 15; pos < 24; pos++) {
    if (mulres[pos] != 0) {
      allzeroes = 0;
    }
  }

  if (allzeroes == 1) {
    for (pos = 4; pos < 15; pos++) {
      cnarr[pos-4] = mulres[pos];
    }
  }
  if (cnarr[11] > 9) {
    for (pos = 0; pos < 10; pos++) {
      cnarr[pos] = 0;
    }
  }

  fixcsizezero();
  fixsignout();
  return;
}

void dividedby() {
  fixsignin();

  csign = 0;
  if (asign != bsign) {
    csign = 1;
  }
  asign = 0;
  bsign = 0;
  bkpcsign = csign;

  protodividedby();

  normdivres();

  for (pos = 0; pos < 12; pos++) {
    cnarr[pos] = divres[pos];
  }

  for (pos = 0; pos <= (alshift + brshift + 12) - blshift ; pos++) {
    for (divi = 0; divi < 11; divi++) {
      subi = cnarr[divi+1]/10;
      subi = subi * 10;
      subi = cnarr[divi+1] - subi;
      subi = subi * 10;
      cnarr[divi] = subi + (cnarr[divi]/10);
    }
    cnarr[11] = cnarr[11]/10;
  }

  csign = bkpcsign;

  fixcsizezero();
  fixsignout();
  return;
}


void squareroot() {
 
  if (anarr[11] > 9) {
    pos = anarr[11] / 10;
    pos = pos * 10;
    anarr[11] = anarr[11] - pos;
  }

  asign = 0;
  bsign = 0;

  for (pos = 0; pos < 12; pos++) {
    bnarr[pos] = 0;
    num[pos] = anarr[pos];
    guess[pos] = anarr[pos];
  }

  for (ionr = 0; ionr < 32; ionr++) {

    for (pos = 0; pos < 12; pos++) {
      anarr[pos] = num[pos];
      bnarr[pos] = guess[pos];
    }

    dividedby();

    for (pos = 0; pos < 12; pos++) {
      anarr[pos] = cnarr[pos];
      bnarr[pos] = guess[pos];
    }

    plus();

    for (pos = 0; pos < 12; pos++) {
      anarr[pos] = cnarr[pos];
      bnarr[pos] = 0;
    }
    bnarr[4] = 2;

    dividedby();

    for (pos = 0; pos < 12; pos++) {
      guess[pos] = cnarr[pos];
    }
  }

  for (pos = 0; pos < 12; pos++) {
    cnarr[pos] = guess[pos];
  }

  fixcsizezero();
  fixsignout();
  csign = 0;
  return;
}

void cuberoot() {
  signbkp = asign;
  fixsignin();

  signbkp = asign;
  asign = 0;
  bsign = 0;
  csign = 0;

  for (cubepos = 0; cubepos < 12; cubepos++) {
    crvary[cubepos] = anarr[cubepos];
    cnarr[cubepos] = anarr[cubepos];
  }

  for (cubeloop = 0; cubeloop < 32 ; cubeloop++) {

    for (cubepos = 0; cubepos < 12; cubepos++) {
      crvarv[cubepos] = cnarr[cubepos];
      anarr[cubepos] = cnarr[cubepos];
      bnarr[cubepos] = cnarr[cubepos];
    }

    times();

    for (cubepos = 0; cubepos < 12; cubepos++) {
      anarr[cubepos] = cnarr[cubepos];
      crvarx[cubepos] = cnarr[cubepos];
      bnarr[cubepos] = crvarv[cubepos];
    }
  
    for (cubepos = 0; cubepos < 12; cubepos++) {
      anarr[cubepos] = cnarr[cubepos];
      bnarr[cubepos] = crvarv[cubepos];
    }

    times();

    for (cubepos = 0; cubepos < 12; cubepos++) {
      anarr[cubepos] = cnarr[cubepos];
      bnarr[cubepos] = crvary[cubepos];
    }

    protominus();

    for (cubepos = 0; cubepos < 12; cubepos++) {
      crvarw[cubepos] = cnarr[cubepos];
      bnarr[cubepos] = 0;
      anarr[cubepos] = crvarx[cubepos];
    }

    bnarr[4] = 3;

    times();

    for (cubepos = 0; cubepos < 12; cubepos++) {
      bnarr[cubepos] = cnarr[cubepos];
      anarr[cubepos] = crvarw[cubepos];
    }

    dividedby();

    for (cubepos = 0; cubepos < 12; cubepos++) {
      bnarr[cubepos] = cnarr[cubepos];
      anarr[cubepos] = crvarv[cubepos];
    }

    protominus();

  }

  fixcsizezero();
  fixsignout();
  csign = signbkp;
  return;
}

void cosapprox() {
    times();
    recordc(datadr3 + datumpos);
    chargea(datadr3 + datumpos);
    for (dati = 0; dati < 12; dati++) {
      crvarv[dati] = anarr[dati];
    }
    chargeb(datadr3 + datumpos);
    times();
    recordc(datadr3 + datumpos);
    chargea(datadr3 + datumpos);
    for (dati = 0; dati < 12; dati++) {
      crvarw[dati] = anarr[dati];
    }
    times();
    recordc(datadr3 + datumpos);
    chargea(datadr3 + datumpos);
    for (dati = 0; dati < 12; dati++) {
      crvarx[dati] = anarr[dati];
    }
    times();
    recordc(datadr3 + datumpos);
    chargea(datadr3 + datumpos);
    for (dati = 0; dati < 12; dati++) {
      crvary[dati] = anarr[dati];
    }
    times();
    recordc(datadr3 + datumpos);
    chargea(datadr3 + datumpos);
    for (dati = 0; dati < 12; dati++) {
      crvarz[dati] = anarr[dati];
    }
    bnarr[11] = 0;
    bnarr[10] = 0;
    bnarr[9] = 0;
    bnarr[8] = 0;
    bnarr[7] = 0;
    bnarr[6] = 0;
    bnarr[5] = 0;
    bnarr[4] = 0;
    bnarr[3] = 27;
    bnarr[2] = 55;
    bnarr[1] = 73;
    bnarr[0] = 19;
    for (dati = 0; dati < 9; dati++) {
      anarr[dati] = anarr[dati + 3];
      anarr[dati + 3] = 0;
    }
    times();
    recordc(datadr3 + datumpos);
    chargea(datadr3 + datumpos);
    for (dati = 0; dati < 12; dati++) {
      crvarz[dati] = anarr[dati];
      anarr[dati] = crvary[dati];
    }
    bnarr[11] = 0;
    bnarr[10] = 0;
    bnarr[9] = 0;
    bnarr[8] = 0;
    bnarr[7] = 0;
    bnarr[6] = 0;
    bnarr[5] = 0;
    bnarr[4] = 24;
    bnarr[3] = 80;
    bnarr[2] = 15;
    bnarr[1] = 87;
    bnarr[0] = 30;
    times();
    recordc(datadr3 + datumpos);
    chargea(datadr3 + datumpos);
    for (dati = 0; dati < 12; dati++) {
      crvary[dati] = anarr[dati];
      anarr[dati] = crvarx[dati];
    }
    bnarr[11] = 0;
    bnarr[10] = 0;
    bnarr[9] = 0;
    bnarr[8] = 0;
    bnarr[7] = 0;
    bnarr[6] = 0;
    bnarr[5] = 13;
    bnarr[4] = 88;
    bnarr[3] = 88;
    bnarr[2] = 88;
    bnarr[1] = 88;
    bnarr[0] = 89;
    times();
    recordc(datadr3 + datumpos);
    chargea(datadr3 + datumpos);
    for (dati = 0; dati < 12; dati++) {
      crvarx[dati] = anarr[dati];
      anarr[dati] = crvarw[dati];
    }
    bnarr[11] = 0;
    bnarr[10] = 0;
    bnarr[9] = 0;
    bnarr[8] = 0;
    bnarr[7] = 0;
    bnarr[6] = 4;
    bnarr[5] = 16;
    bnarr[4] = 66;
    bnarr[3] = 66;
    bnarr[2] = 66;
    bnarr[1] = 66;
    bnarr[0] = 67;
    times();
    recordc(datadr3 + datumpos);
    chargea(datadr3 + datumpos);
    for (dati = 0; dati < 12; dati++) {
      crvarw[dati] = anarr[dati];
      anarr[dati] = crvarv[dati];
    }
    bnarr[11] = 0;
    bnarr[10] = 0;
    bnarr[9] = 0;
    bnarr[8] = 0;
    bnarr[7] = 0;
    bnarr[6] = 50;
    bnarr[5] = 0;
    bnarr[4] = 0;
    bnarr[3] = 0;
    bnarr[2] = 0;
    bnarr[1] = 0;
    bnarr[0] = 0;
    times();
    recordc(datadr3 + datumpos);
    chargea(datadr3 + datumpos);
    for (dati = 0; dati < 12; dati++) {
      bnarr[dati] = anarr[dati];
      anarr[dati] = 0;
    }
    anarr[7] = 1;
    minus();
    recordc(datadr3 + datumpos);
    chargea(datadr3 + datumpos);
    for (dati = 0; dati < 12; dati++) {
      bnarr[dati] = crvarw[dati];
    }
    plus();
    recordc(datadr3 + datumpos);
    chargea(datadr3 + datumpos);
    for (dati = 0; dati < 12; dati++) {
      bnarr[dati] = crvarx[dati];
    }
    minus();
    recordc(datadr3 + datumpos);
    chargea(datadr3 + datumpos);
    for (dati = 0; dati < 12; dati++) {
      bnarr[dati] = crvary[dati];
    }
    plus();
    recordc(datadr3 + datumpos);
    chargea(datadr3 + datumpos);
    for (dati = 0; dati < 12; dati++) {
      bnarr[dati] = crvarz[dati];
    }
    minus();
    for (dati = 0; dati < 9; dati++) {
      cnarr[dati] = cnarr[dati + 3];
      cnarr[dati + 3] = 0;
    }
    recordc(datadr3 + datumpos);
    datumpos++;

  return;
}


void main() {

/* trace by default */
traceon = 1;

/* maximum count of instructions prior to forced termination */
/* to prevent unintended infinite loops */
runlimit = 3600; 

while (exitflag == 0) {

while (1) {
pc = 0; 

for (posi = 0; posi < 30; posi++) {
  prnnum(0);
}
prnnl();

cmdadr = readint();

if (cmdadr < 220) {

  opr = readint();
  
  datadr1 = readint();
  
  datadr2 = readint();
  
  datadr3 = readint();

  instruction[cmdadr * 4] = opr;
  instruction[(cmdadr * 4) + 1] = datadr1;
  instruction[(cmdadr * 4) + 2] = datadr2;
  instruction[(cmdadr * 4) + 3] = datadr3;
  frontzero(cmdadr);
  if (opr < 10) {
    prnnum(0);
  }
  prnnumspace(opr);
  frontzero(datadr1);
  frontzero(datadr2);
  frontzero(datadr3);
  prnnl();

if (cmdadr == 0) {
    break; 
  }
}  else if (cmdadr == 1111) {

  i = readint();
  for (j = i; j < i + 4; j = j + 1) {
    frontzero(j);
    if (instruction[j * 4] < 10) {
      prnnum(0);
    }
    prnnumspace(instruction[j * 4]);
    frontzero(instruction[(j * 4) + 1]);
    frontzero(instruction[(j * 4) + 2]);
    frontzero(instruction[(j * 4) + 3]);
    prnnl();
  }

}  else if (cmdadr == 2222) {
  i = readint();

  for (j = i; j < i + 4; j = j + 1) {
    if (j > 219) {
      break;
    }
    prndatum(j);
    prnnl();
  }


} else if (cmdadr == 4444) {

  dati = readint();

  for (posi = 6 + (dati * 6); posi > (dati * 6); posi--) {
    ionr = readint();
    ioshift = ionr / 10000;
    ioshift = ioshift * 10000;
    ionr = ionr - ioshift;
    datum[posi-1] = ionr;
    for (datumpos = 6 + (dati * 6); datumpos >= posi; datumpos--) {
      frontzero(datum[datumpos-1]);
    }
    prnnl();
  }

} else if (cmdadr == 5555) {
  /* clear all instructions */

  for (i = 0; i < 880; i++) {
    instruction[i] = 0;
  }

} else if (cmdadr == 6666) {
  /* clear all data */

  for (i = 0; i < 2640; i++) {
    datum[i] = 0;
  }

} else if (cmdadr == 7777) {
  /* toggle execution tracing */

  if (traceon == 0) {
    traceon = 1;
  } else {
    traceon = 0;
  }
  displaynumber(traceon);

} else if (cmdadr == 8888) {
  /* exit */
  exitflag = 1;
  break;

} else if (cmdadr == 9999) {
  /* set new run limit, i.e. new maximum instruction run count */
  /* a run limit of 0 disables the safety */

  runlimit = readint();
}

}

exitexec = 0;

if (exitflag == 0) {
while (pc < 220) {

  /* terminate execution upon exhaustion of the run limit */
  /* "safety" against infinite loops, forcing them to terminate anyway */
  if (runlimit == 1) {
    break;
  } else if (runlimit > 1) {
    runlimit--;
  }

  for (posi = 0; posi < 6; posi++) {
    datum[posi] = 0;
  }

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

  /* trace, if requested by traceon */
  if (traceon == 1) {
    frontzero(pc);
    if (opr < 10) {
      prnnum(0);
    }
    prnnumspace(opr);
    frontzero(datadr1);
    frontzero(datadr2);
    frontzero(datadr3);
    prnnl();
  }
  
  if (opr == 0) {
  /* an operand of zero is ignored */
  /* useful for "commenting out" instructions */

  } else if (opr == 1) {
  /* JUMP -- JUMP INSTRUCTION */
  /* datadr3 decides which of the jumps to test against */
  /* 6 and 13 signify unconditional jumps, ignoring datadr1 */
  /* datadr1 decides which comparison against 0 to execute */
  /* datadr2 gives either directly the jump address (itself, not its value) */
  /* or it allows "indirect" jumps to datadr2's value - a computed goto */

    k = 0;

    /* "dead" code for now - prepare to execute a far jump */
    /* here - not used, the system has not enough instruction range */
    /* for far jumps to be an issue */
    if (datadr3 > 13) {
      datadr3 = datadr3 - 14;
      k = 10000;
    }

    chargea(datadr1);

    allzeroes = 1;
    for (posi = 1; posi < 12; posi++) {
      if (anarr[posi] != 0) {
        allzeroes = 0;
        break;
      }
    }

    subi = 0;

    /* "simple goto": jump at the instruction number equal to datadr2 */
    if (((allzeroes == 1) && (datadr3 == 0)) ||
        ((allzeroes == 0) && (anarr[11] < 10) && (datadr3 == 1)) ||
        ((anarr[11] > 9) && (datadr3 == 2)) ||
        ((anarr[11] < 10) && (datadr3 == 3)) ||
        (((anarr[11] > 9) || (allzeroes == 1)) && (datadr3 == 4)) ||
        ((allzeroes == 0) && (datadr3 == 5)) ||
        (datadr3 == 6)) {

      pc = datadr2 + k; 
      k = 0;
      subi = 1;
    }

    /* "computed goto": jump to (part of) the VALUE at the ADDRESS datadr2 */
    if (subi == 0) {
      if (((allzeroes == 1) && (datadr3 == 7)) ||
          ((allzeroes == 0) && (anarr[11] < 10) && (datadr3 == 8)) ||
          ((anarr[11] > 9) && (datadr3 == 9)) ||
          ((anarr[11] < 10) && (datadr3 == 10)) ||
          (((anarr[11] > 9) || (allzeroes == 1)) && (datadr3 == 11)) ||
          ((allzeroes == 0) && (datadr3 == 12)) ||
          (datadr3 == 13)) {

        chargeb(datadr2);

        /* bnarr[{0,1,2,3}] are floating point positions - ignore these */
        /* use bnarr[{4,5,6}] to signify the jump target */
        /* higher integer value parts are ignored: */
        /* there is no more instruction memory anyway */ 
        pc = bnarr[4] + (100 * bnarr[5]) + (10000 * bnarr[6]) + k; 
        k = 0;
        subi = 1;
      }
    }

    if (subi == 0) { pc++; }

    subi = 0;

  } else if (opr == 2) {
  /* IADR -- INDIRECT ADDRESSING: */
    /* substitute in the NEXT instruction datadr1, datadr2 and datadr3 */
    /* by the respective values found therein - for indirect addressing */
    h = datum[2 + (datadr1 * 6)] + (10000 * datum[3 + (datadr1 * 6)]);
    k = datum[2 + (datadr2 * 6)] + (10000 * datum[3 + (datadr2 * 6)]);
    l = datum[2 + (datadr3 * 6)] + (10000 * datum[3 + (datadr3 * 6)]);

  } else if (opr == 5) {
  /* SADR -- SET ADDRESS AS VALUE: */
  /* This can also used to let an address be treated as a value in some */
  /* future instruction - IADR (opcode 2) can help reversing the process. */

    ioshift = datadr1 / 10000;
    ioshift = ioshift * 10000;
    datadr1 = datadr1 - ioshift;

    ioshift = datadr2 / 10000;
    ioshift = ioshift * 10000;
    datadr2 = datadr2 - ioshift;

    datum[2 + (datadr3 * 6)] = datadr1;
    datum[1 + (datadr3 * 6)] = datadr2;

    /* 0,5,1234,4567,3 sets datum 3 to 1234.4567 */

  } else if (opr == 6) {
  /* SVAL -- SET VALUE: This is the "normal 'equal'" in other languages. */
  /* Did not call it EQUL or thelike, to help visually differentiate */
  /* values and addresses. */
  /* Essentially: datum[datadr3] = datum[datadr2] */

    dati = datadr2 * 6;
    for (posi = datadr3 * 6; posi < (datadr3 * 6) + 6; posi++) {
      datum[posi] = datum[dati];
      dati++;
    }
    
  } else if (opr == 8) {
  /* IVAS -- indirectly addressed assignment. */
  /* Essentially: datum[datum[datadr3]] = datum[datadr2] */

    dati = datum[2 + (datadr3 * 6)] + (10000 * datum[3 + (datadr3 * 6)]);
    for (posi = datadr2 * 6; posi < (datadr2 * 6) + 6; posi++) {
      datum[dati] = datum[posi];
      dati++;
    }

  } else if (opr == 9) {
  /* PLUS -- PLUS FOR SINGLE NUMBERS, NOT RANGE. */
  /* arithmetics are all: value-of-1 operation value-of-2 --> value-of-3 */

    chargea(datadr1);
    chargeb(datadr2);
    plus();
    recordc(datadr3);

  } else if (opr == 10) {
  /* MINS -- MINUS FOR SINGLE NUMBERS, NOT RANGE. */

    chargea(datadr1);
    chargeb(datadr2);
    minus();
    recordc(datadr3);

  } else if (opr == 11) {
  /* MULS -- MULTIPLY SINGLE NUMBERS, NOT RANGE. */

    chargea(datadr1);
    chargeb(datadr2);
    times();
    recordc(datadr3);

  } else if (opr == 12) {
  /* DIVS -- DIVIDE SINGLE NUMBERS, NOT RANGE. */
  /* In case of a division by 0, there shall be no hysteria. Just give 0. */
  /* This makes loops much easier than "having to look out for the */
  /* mathematical booby-trap" all the time. Here this is done GENERALLY. */
    chargea(datadr1);
    chargeb(datadr2);
    dividedby();
    recordc(datadr3);

  } else if (opr == 13) {
  /* POXY -- X TO THE YTH */
  /* value-of-1 to the positive int. power of value-of-2 gives value-of-3 */

    chargeb(datadr2);

    if (bnarr[11] > 9) {
      pos = bnarr[11]/10;
      bnarr[11] = bnarr[11] - (pos * 10);
    }

    if ((bnarr[1] == 99) && (bnarr[2] == 99) && (bnarr[3] == 99)) {
      for (posi = 0; posi < 12; posi++) {
        anarr[posi] = 0;
      }
      anarr[4] = 1;
      plus();
      for (posi = 4; posi < 12; posi++) {
        bnarr[posi] = cnarr[posi];
      }
    }
    for (posi = 0; posi < 4; posi++) {
      bnarr[posi] = 0;
    }

    chargea(datadr1);

    for (posi = 0; posi < 12; posi++) {
      cnarr[posi] = 0;
      num[posi] = bnarr[posi];
    }
    cnarr[4] = 1;
    csign = asign;

    recordc(datadr3);

    biszero = 1;
    for (posi = 0; posi < 12; posi++) {
      if (bnarr[posi] != 0) {
        biszero = 0;
        break;
      }
    }

    while (biszero != 1) {
      for (posi = 0; posi < 12; posi++) {
        anarr[posi] = num[posi];
      }
      asign = 0;

      for (posi = 0; posi < 12; posi++) {
        bnarr[posi] = 0;
      }

      bnarr[4] = 1;

      minus();

      for (posi = 0; posi < 12; posi++) {
        num[posi] = cnarr[posi];
      }

      biszero = 1;
      for (posi = 0; posi < 12; posi++) {
        if (cnarr[posi] != 0) {
          biszero = 0;
          break;
        }
      }

      chargea(datadr1);
      chargeb(datadr3);
      times();
      recordc(datadr3);
    }

  } else if (opr == 15) {
  /* IFRA -- INTEGER AND FRACTIONAL PART */
  /* The integer part of value-of-datadr1 is stored in the value-of-datadr2 */
  /* and the fractional part of value-of-datadr1 in value-of-datadr3 */

    datum[(datadr3 * 6) + 5] = datum[(datadr1 * 6) + 5];
    datum[(datadr3 * 6) + 4] = datum[(datadr1 * 6) + 4];
    datum[(datadr3 * 6) + 3] = datum[(datadr1 * 6) + 3];
    datum[(datadr3 * 6) + 2] = datum[(datadr1 * 6) + 2];
    datum[(datadr3 * 6) + 1] = 0;
    datum[datadr3 * 6] = 0;

    if (datum[(datadr1 * 6) + 5] >= 1000) {
      datum[(datadr2 * 6) + 5] = 1000;
    } else {
      datum[(datadr2 * 6) + 5] = 0;
    }
    datum[(datadr2 * 6) + 4] = 0;
    datum[(datadr2 * 6) + 3] = 0;
    datum[(datadr2 * 6) + 2] = 0;
    datum[(datadr2 * 6) + 1] = datum[(datadr1 * 6) + 1];
    datum[datadr2 * 6] = datum[datadr1 * 6];

  } else if (opr == 17) {
  /* AMNT -- AMOUNT WITH PERCENTAGE OVER TIME */
  /* 1 = period 2 = +-percentage as decimal 3 = (1+percentage)^period */
  /* according to the equation, amount = (1 + (percentage / 100)) ^ period */

    chargea(datadr1);
    chargeb(datadr2);

    for (posi = 0; posi < 4; posi++) {
      anarr[posi] == 0;
    }

    signbkp = 0;
    if (bnarr[11] > 9) {
      signbkp = 1;
      bnarr[11] = bnarr[11] - 10;
    }

    allzeroes = 1;
    for (posi = 0; posi < 12; posi++) {
      if (bnarr[posi] != 0) {
        allzeroes = 0;
      }
      crvarv[posi] = anarr[posi];
    }
    bsign = 0;
    asign = 0;

    for (posi = 0; posi < 12; posi++) {
      anarr[posi] = 0;
      crvary[posi] = 0;
    }
    anarr[4] = 1;
    crvary[4] = 1;

    if (allzeroes == 1) {
      for (posi = 0; posi < 12; posi++) {
        cnarr[posi] = 0;
      }
      cnarr[4] = 1;
      recordc(datadr3);
      break;
    }

    while (allzeroes == 0) {

      asign = 0;
      bsign = 0;

      times();

      for (posi = 0; posi < 12; posi++) {
        crvarw[posi] = bnarr[posi];
        bnarr[posi] = cnarr[posi];

        anarr[posi] = crvary[posi];
      }

      asign = 0;
      bsign = 0;

      if (signbkp == 1) {
        minus();
      } else {
        plus();
      }

      for (posi = 0; posi < 12; posi++) {
        crvary[posi] = cnarr[posi];

        anarr[posi] = crvarv[posi];
        bnarr[posi] = 0;
      }
      bnarr[4] = 1;

      minus();

      allzeroes = 1;
      for (posi = 0; posi < 12; posi++) {
        if (cnarr[posi] != 0) {
          allzeroes = 0;
        }
        crvarv[posi] = cnarr[posi];
      }

      if (allzeroes == 1) {
        for (posi = 0; posi < 12; posi++) {
          cnarr[posi] = crvary[posi];
        }
        recordc(datadr3);
        break;
      }
    
      for (posi = 0; posi < 12; posi++) {
        anarr[posi] = crvary[posi];
        bnarr[posi] = crvarw[posi];
      }

    }

  } else if (opr == 20) {
  /* SWAP -- SWAP TO NUMBERS */
  /* Namely those at datadr 1 and 2. */
  /* A trivial but annoying task for novices. */

    for (posi = 0; posi < 6; posi++) {
      dati = datum[(datadr1 * 6) + posi];
      datum[(datadr1 * 6) + posi] = datum[(datadr2 * 6) + posi];
      datum[(datadr2 * 6) + posi] = dati;
    }

  } else if (opr == 22) {
  /* COPY -- COPY A RANGE */
  /* Quasi "to copy arrays": 1 = rangebegin 2 = rangeend 3 = targetbegin */
  /* Limits are all inclusive. */

    datumpos = 0;
    for (posi = datadr1; posi <= datadr2; posi++) {
      chargea(posi);

      for (dati = 0; dati < 12; dati++) {
        cnarr[dati] = anarr[dati];
      }

      recordc(datadr3 + datumpos);
      datumpos++;
    }

  } else if (opr == 23) {
  /* FRIS -- FILL RANGE BY STEP */
  /* The inclusive range between datadr1 and 2 is filled by the step at 3 */
  /* i.e. if the step is 2, then the filling is 2,4,6,8... */

    if (datadr2 > datadr1) {
      chargeb(datadr3);
      for (posi = datadr1 + 1; posi <= datadr2; posi++) {
        chargea(posi - 1);
        plus();
        recordc(posi);
      }
    }

  } else if (opr == 25) {
  /* SORT */
  /* Sort the range between datadr1 and datadr2, ignoring datadr3 */

    subi = 1;
    while (subi == 1) {
      for (posi = datadr1; posi < datadr2; posi++) {
        subi = 0;
        chargea(posi+1);
        chargeb(posi);
        minus();
        if (cnarr[11] > 9) {
          subi = 1;
          chargea(posi+1);
          chargeb(posi);
          for (dati = 0; dati < 12; dati++) {
            cnarr[dati] = anarr[dati];
          }
          recordc(posi);
          for (dati = 0; dati < 12; dati++) {
            cnarr[dati] = bnarr[dati];
          }
          recordc(posi + 1);
          break;
        }
      }
    }

  } else if (opr == 26) {
  /* CORS */
  /* Coupled range sorting - this is a facility for sorting one range */
  /* according to another. For instance, you have employee age and */
  /* salary for employees of a certain type in two ranges of equal */
  /* size, and you would like to sort the one range by age and see how */
  /* the second range, that of the salaries, would change according to */
  /* that age sorting. (Evidently, JUST sorting the age or JUST sorting */
  /* the salaries is NOT what you want - you want to keep the COUPLING */
  /* of each pair of elements in the respective ranges.) */
  /* 1 = rangebegin 2 = rangeend 3 = targetbegin */

    subi = 1;
    while (subi == 1) {
      datumpos = 0;
      for (posi = datadr1; posi < datadr2; posi++) {
        subi = 0;
        chargea(posi+1);
        chargeb(posi);
        minus();
        if (cnarr[11] > 9) {
          subi = 1;
          chargea(posi+1);
          chargeb(posi);
          for (dati = 0; dati < 12; dati++) {
            cnarr[dati] = anarr[dati];
          }
          recordc(posi);
          for (dati = 0; dati < 12; dati++) {
            cnarr[dati] = bnarr[dati];
          }
          recordc(posi + 1);

          chargea(datadr3 + datumpos + 1);
          chargeb(datadr3 + datumpos);
          for (dati = 0; dati < 12; dati++) {
            cnarr[dati] = anarr[dati];
          }
          recordc(datadr3 + datumpos);
          for (dati = 0; dati < 12; dati++) {
            cnarr[dati] = bnarr[dati];
          }
          recordc(datadr3 + datumpos + 1);
          break;
        }
      }
      datumpos++;
    }

  } else if (opr == 27) {
  /* TURN -- REVERSE A RANGE, I.E. TURN IT UPSIDE DOWN */
  /* 1 = rangebegin 2 = rangeend 3 = ignored, result in range */

    while (datadr1 < datadr2) {
      chargea(datadr1);
      chargeb(datadr2);
      for (posi = 0; posi < 12; posi++) {
        cnarr[posi] = anarr[posi];
      }
      recordc(datadr2);
      for (posi = 0; posi < 12; posi++) {
        cnarr[posi] = bnarr[posi];
      }
      recordc(datadr1);
      datadr1++;
      datadr2--;
    }

  } else if (opr == 28) {
  /* SUMR -- SUM UP A RANGE INTO ONE NUMBER */
  /* 1 = rangebegin 2 = rangeend 3 = result */

    for (dati = 0; dati < 12; dati++) {
       cnarr[dati] = 0;
    }

    for (posi = datadr1; posi <= datadr2; posi++) {
      chargea(posi);

      for (dati = 0; dati < 12; dati++) {
        bnarr[dati] = cnarr[dati];
        cnarr[dati] = 0;
      }

      plus();
    }

    recordc(datadr3);

  } else if (opr == 29) {
  /* SUSQ -- SUM THE SQUARES OF A RANGE INTO ONE NUMBER */
  /* Useful for computing variance, geometric distances and thelike. */
  /* 1 = rangebegin 2 = rangeend 3 = result */

    for (dati = 0; dati < 12; dati++) {
      cnarr[dati] = 0;
    }

    recordc(datadr3);

    for (posi = datadr1; posi <= datadr2; posi++) {
      chargea(posi);
      chargeb(posi);
      times();
      for (dati = 0; dati < 12; dati++) {
        anarr[dati] = cnarr[dati];
      }
      chargeb(datadr3);
      plus();
      recordc(datadr3);
    }

  } else if (opr == 30) {
  /* IXTH -- 1/X */
  /* Turn each X within a range to 1/Xth. If the value is zero, 1/Xth */
  /* of it is assumed to be 0, too. */

    datumpos = 0;
    for (posi = datadr1; posi <= datadr2; posi++) {
      for (dati = 0; dati < 12; dati++) {
        anarr[dati] = 0;
      }
      anarr[4] = 1;

      chargeb(posi);
      dividedby();
      recordc(datadr3 + datumpos);

      datumpos++;
    }

  } else if (opr == 31) {
  /* ABSR -- ABSOLUTE VALUES OF A RANGE */
  /* 1 = rangebegin 2 = rangeend 3 = targetbegin */

    datumpos = 0;
    for (posi = datadr1; posi <= datadr2; posi++) {
      chargea(posi);
      if (anarr[11] > 9) {
        pos = anarr[11]/10;
        anarr[11] = anarr[11] - (pos * 10);
        asign = 1;
      }
      csign = 0;
      for (dati = 0; dati < 12; dati++) {
        cnarr[dati] = anarr[dati];
      }
      recordc(datadr3 + datumpos);
      datumpos++;
    }

  } else if (opr == 32) {
  /* SQRT -- COMPUTE THE SQUARE ROOTS OF A RANGE */
  /* 1 = rangebegin 2 = rangeend 3 = targetbegin */

    datumpos = 0;
    for (posi = datadr1; posi <= datadr2; posi++) {
      chargea(posi);
      squareroot();
      recordc(datadr3 + datumpos);
      datumpos++;
    }

  } else if (opr == 33) {
  /* SQUA -- SQUARE EACH NUMBER IN A RANGE */
  /* 1 = rangebegin 2 = rangeend 3 = targetbegin */

    datumpos = 0;
    for (posi = datadr1; posi <= datadr2; posi++) {
      chargea(posi);
      chargeb(posi);
      times();
      recordc(datadr3 + datumpos);
      datumpos++;
    }

  } else if (opr == 34) {
  /* CBRT -- COMPUTE THE THIRD ROOT OF EACH NUMBER IN A RANGE */
  /* 1 = rangebegin 2 = rangeend 3 = targetbegin */

    datumpos = 0;
    for (posi = datadr1; posi <= datadr2; posi++) {
      chargea(posi);
      cuberoot();
      recordc(datadr3 + datumpos);
      datumpos++;
    }

  } else if (opr == 35) {
  /* CUBE -- RAISE EACH NUMBER IN A RANGE TO THE THIRD POWER */
  /* 1 = rangebegin 2 = rangeend 3 = targetbegin */

    datumpos = 0;
    for (posi = datadr1; posi <= datadr2; posi++) {
      chargea(posi);
      chargeb(posi);
      times();
      recordc(datadr3 + datumpos);
      chargea(datadr3 + datumpos);
      chargeb(posi);
      times();
      recordc(datadr3 + datumpos);
      datumpos++;
    }

  } else if (opr == 40) {
  /* SIND -- "DECIMAL" SINUS OF A RANGE */
  /* I.e. assume the numbers are given in 0-90 degrees, not min and sec */
  /* 1 = rangebegin 2 = rangeend 3 = targetbegin */
  /* The trigonometric functions' results are not particularly reliable. */

    datumpos = 0;
    for (posi = datadr1; posi <= datadr2; posi++) {
      anarr[11] = 0;
      anarr[10] = 0;
      anarr[9] = 0;
      anarr[8] = 0;
      anarr[7] = 0;
      anarr[6] = 0;
      anarr[5] = 0;
      anarr[4] = 90;
      anarr[3] = 0;
      anarr[2] = 0;
      anarr[1] = 0;
      anarr[0] = 0;
      chargeb(posi);
      minus();
      recordc(datadr3 + datumpos);
      chargea(datadr3 + datumpos);
      bnarr[11] = 0;
      bnarr[10] = 0;
      bnarr[9] = 0;
      bnarr[8] = 0;
      bnarr[7] = 0;
      bnarr[6] = 0;
      bnarr[5] = 0;
      bnarr[4] = 0;
      bnarr[3] = 1;
      bnarr[2] = 74;
      bnarr[1] = 53;
      bnarr[0] = 29;
      times();
      recordc(datadr3 + datumpos);
      chargea(datadr3 + datumpos);
      chargeb(datadr3 + datumpos);
      cosapprox();
    }

  } else if (opr == 41) {
  /* SIND -- "DECIMAL" COSINUS OF A RANGE */
  /* I.e. assume the numbers are given in 0-90 degrees, not min and sec */
  /* 1 = rangebegin 2 = rangeend 3 = targetbegin */
  /* The trigonometric functions' results are not particularly reliable. */

    datumpos = 0;
    for (posi = datadr1; posi <= datadr2; posi++) {
      chargea(posi);
      bnarr[11] = 0;
      bnarr[10] = 0;
      bnarr[9] = 0;
      bnarr[8] = 0;
      bnarr[7] = 0;
      bnarr[6] = 0;
      bnarr[5] = 0;
      bnarr[4] = 0;
      bnarr[3] = 1;
      bnarr[2] = 74;
      bnarr[1] = 53;
      bnarr[0] = 29;
      times();
      recordc(datadr3 + datumpos);
      chargea(datadr3 + datumpos);
      chargeb(datadr3 + datumpos);
      cosapprox();
    }

  } else if (opr == 42) {
  /* TAND -- "DECIMAL" TANGENS OF A RANGE */
  /* I.e. assume the numbers are given in 0-90 degrees, not min and sec */
  /* 1 = rangebegin 2 = rangeend 3 = targetbegin */
  /* The trigonometric functions' results are not particularly reliable. */

    datumpos = 0;
    for (posi = datadr1; posi <= datadr2; posi++) {
      anarr[11] = 0;
      anarr[10] = 0;
      anarr[9] = 0;
      anarr[8] = 0;
      anarr[7] = 0;
      anarr[6] = 0;
      anarr[5] = 0;
      anarr[4] = 90;
      anarr[3] = 0;
      anarr[2] = 0;
      anarr[1] = 0;
      anarr[0] = 0;
      chargeb(posi);
      minus();
      recordc(datadr3 + datumpos);
      chargea(datadr3 + datumpos);
      bnarr[11] = 0;
      bnarr[10] = 0;
      bnarr[9] = 0;
      bnarr[8] = 0;
      bnarr[7] = 0;
      bnarr[6] = 0;
      bnarr[5] = 0;
      bnarr[4] = 0;
      bnarr[3] = 1;
      bnarr[2] = 74;
      bnarr[1] = 53;
      bnarr[0] = 29;
      times();
      recordc(datadr3 + datumpos);
      chargea(datadr3 + datumpos);
      chargeb(datadr3 + datumpos);
      cosapprox();

      datumpos--;
      chargea(datadr3 + datumpos);
      for (dati = 0; dati < 12; dati++) {
        num[dati] = anarr[dati];
      }

      chargea(posi);
      bnarr[11] = 0;
      bnarr[10] = 0;
      bnarr[9] = 0;
      bnarr[8] = 0;
      bnarr[7] = 0;
      bnarr[6] = 0;
      bnarr[5] = 0;
      bnarr[4] = 0;
      bnarr[3] = 1;
      bnarr[2] = 74;
      bnarr[1] = 53;
      bnarr[0] = 29;
      times();
      recordc(datadr3 + datumpos);
      chargea(datadr3 + datumpos);
      chargeb(datadr3 + datumpos);
      cosapprox();

      datumpos--;
      chargeb(datadr3 + datumpos);
      for (dati = 0; dati < 12; dati++) {
        anarr[dati] = num[dati];
      }

      dividedby();

      recordc(datadr3 + datumpos);
      datumpos++;
    }

  } else if (opr == 46) {
  /* MSTD -- MEAN AND STANDARD DEVIATION */
  /* 1 = rangebegin 2 = rangeend 3 = mean, 3++ stdev */
  /* Division is by n, not n-1 */

    for (posi = datadr1; posi <= datadr2; posi++) {
      cnarr[posi] = 0;
    }
    recordc(datadr3);

    for (posi = datadr1; posi <= datadr2; posi++) {
      chargea(posi);
      chargeb(datadr3);
      plus();
      recordc(datadr3);
    }

    chargea(datadr3);

    subi = 1 + datadr2 - datadr1;

    bnarr[11] = 0;
    bnarr[10] = 0;
    bnarr[9] = 0;
    bnarr[8] = 0;
    bnarr[7] = 0;
    bnarr[6] = subi/10000;
    bnarr[5] = (subi/100) - (bnarr[6] * 100);
    bnarr[4] = (subi - (bnarr[5] * 100)) - (bnarr[6] * 10000);
    bnarr[3] = 0;
    bnarr[2] = 0;
    bnarr[1] = 0;
    bnarr[0] = 0;

    dividedby();

    recordc(datadr3);

    datadr3++;

    for (posi = datadr1; posi <= datadr2; posi++) {
      cnarr[posi] = 0;
      crvarv[posi] = 0;
    }

    recordc(datadr3);

    for (posi = datadr1; posi <= datadr2; posi++) {
      chargea(posi);
      chargeb(datadr3 - 1);
      minus();
      recordc(datadr3);

      chargea(datadr3);
      chargeb(datadr3);
      times();
      recordc(datadr3);

      chargea(datadr3);
      for (dati = 0; dati < 12; dati++) {
        bnarr[dati] = crvarv[dati];
      }
      plus();
      for (dati = 0; dati < 12; dati++) {
        crvarv[dati] = cnarr[dati];
      }
    }

    recordc(datadr3);

    subi = datadr2 - datadr1;;

    bnarr[11] = 0;
    bnarr[10] = 0;
    bnarr[9] = 0;
    bnarr[8] = 0;
    bnarr[7] = 0;
    bnarr[6] = subi/10000;
    bnarr[5] = (subi/100) - (bnarr[6] * 100);
    bnarr[4] = (subi - (bnarr[5] * 100)) - (bnarr[6] * 10000);
    bnarr[3] = 0;
    bnarr[2] = 0;
    bnarr[1] = 0;
    bnarr[0] = 0;

    chargea(datadr3);

    dividedby();
    recordc(datadr3);

    chargea(datadr3);
    squareroot();
    recordc(datadr3);

  } else if (opr == 53) {
  /* PLUR -- ADD TWO RANGES */
  /* 1 = rangebegin 2 = rangeend 3 = begin of second range, */
  /* result in first range */

    datumpos = 0;
    for (posi = datadr1; posi <= datadr2; posi++) {
      chargea(posi);
      chargeb(datadr3 + datumpos);
      plus();
      recordc(posi);
      datumpos++;
    }

  } else if (opr == 54) {
  /* MINR -- ONE RANGE MINUS ANOTHER RANGE */
  /* 1 = rangebegin 2 = rangeend 3 = begin of second range, */
  /* result in first range */

    datumpos = 0;
    for (posi = datadr1; posi <= datadr2; posi++) {
      chargea(posi);
      chargeb(datadr3 + datumpos);
      minus();
      recordc(posi);
      datumpos++;
    }

  } else if (opr == 55) {
  /* MULR -- MULTIPLY EACH PAIR OF NUMBERS IN TWO RANGES */
  /* 1 = rangebegin 2 = rangeend 3 = begin of second range, */
  /* result in first range */

    datumpos = 0;
    for (posi = datadr1; posi <= datadr2; posi++) {
      chargea(posi);
      chargeb(datadr3 + datumpos);
      times();
      recordc(posi);
      datumpos++;
    }

  } else if (opr == 56) {
  /* DIVR -- DIVIDE EACH PAIR OF NUMBERS IN TWO RANGES */
  /* 1 = rangebegin 2 = rangeend 3 = begin of second range, */
  /* result in first range */

    datumpos = 0;
    for (posi = datadr1; posi <= datadr2; posi++) {
      chargea(posi);
      chargeb(datadr3 + datumpos);
      dividedby();
      recordc(posi);
      datumpos++;
    }

  } else if (opr == 57) {
  /* PLUN -- ADD A NUMBER TO EACH ELEMENT OF A RANGE */
  /* 1 = rangebegin 2 = rangeend 3 = num, result in range */

    for (posi = datadr1; posi <= datadr2; posi++) {
      chargeb(datadr3);
      chargea(posi);
      plus();
      recordc(posi);
    }

  } else if (opr == 58) {
  /* MINN -- SUBTRACT A NUMBER FROM EACH ELEMENT OF A RANGE */
  /* 1 = rangebegin 2 = rangeend 3 = num, result in range */

    for (posi = datadr1; posi <= datadr2; posi++) {
      chargeb(datadr3);
      chargea(posi);
      minus();
      recordc(posi);
    }

  } else if (opr == 59) {
  /* MULN -- MULTIPLY EACH ELEMENT OF A RANGE BY A NUMBER */
  /* 1 = rangebegin 2 = rangeend 3 = num, result in range */

    for (posi = datadr1; posi <= datadr2; posi++) {
      chargeb(datadr3);
      chargea(posi);
      times();
      recordc(posi);
    }

  } else if (opr == 60) {
  /* DIVN -- DIVIDE EACH ELEMENT OF A RANGE BY A NUMBER */
  /* 1 = rangebegin 2 = rangeend 3 = num, result in range */

    for (posi = datadr1; posi <= datadr2; posi++) {
      chargeb(datadr3);
      chargea(posi);
      dividedby();
      recordc(posi);
    }

  }

  /* Proceed to the next instruction unless we have a jump (1) or */
  /* an immediate operation (0) -- in the latter case, simply return */
  /* to interactive mode. An unconditional interactive jump can be used to */
  /* start execution at some desired address. */

  if (opr != 1) {
    if (pc > 0) {
      pc++;
    } else if (pc == 0) {

      pc = 220;
    }
  }

}

}

}

return;
}


