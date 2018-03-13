package brs.at;

public interface AT_API {
  // range 0x0100..0x01ff
  /**
   * sets @addr to A1 (0x0100)
   * @param state AT machine state
   * @return A1 register
   *
   */
  long get_A1(AT_Machine_State state);

  /**
   *  sets @addr to A2 (0x0101)
   * @param state AT machine state
   * @return A2 register
   */
  long get_A2(AT_Machine_State state);

  /**
   * sets @addr to A3 (0x0102)
   * @param state AT machine state
   * @return A3 register
   */
  long get_A3(AT_Machine_State state);

  /**
   * sets @addr to A4 (0x0103)
   * @param state AT machine state
   * @return A4 register
   */
  long get_A4(AT_Machine_State state);

  /**
   * sets @addr to B1 (0x0104)
   * @param state AT machine state
   * @return B1 register
   */
  long get_B1(AT_Machine_State state);

  /**
   * sets @addr to B2 (0x0105)
   * @param state AT machine state
   * @return B2 register
   */
  long get_B2(AT_Machine_State state);

  /**
   * sets @addr to B3 (0x0106)
   * @param state AT machine state
   * @return B3 register
   */
  long get_B3(AT_Machine_State state);

  /**
   * sets @addr to B4 (0x0107)
   * @param state AT machine state
   * @return B4 register
   */
  long get_B4(AT_Machine_State state);

  /**
   * sets A1 to @addr (0x0110)
   * @param val new A1 register value
   * @param state AT machine state
   *
   */
  void set_A1(long val, AT_Machine_State state);

  /**
   *  sets A2 to @addr (0x0111)
   * @param val new A2 register value
   * @param state AT machine state
   */
  void set_A2(long val, AT_Machine_State state);

  /**
   * sets A3 to @addr (0x0112)
   * @param val new A3 register value
   * @param state AT machine state
   */
  void set_A3(long val, AT_Machine_State state);

  /**
   * sets A4 to @addr (0x0113)
   * @param val new A4 register value
   * @param state AT machine state
   */
  void set_A4(long val, AT_Machine_State state);

  /**
   * sets A1 from @addr1 and A2 from @addr2 (0x0114)
   * @param val1 new A1 register value
   * @param val2 new A2 register value
   * @param state AT machine state
   */
  void set_A1_A2(long val1, long val2, AT_Machine_State state);

  /**
   * sets A3 from @addr1 and A4 from @addr2 ((0x0115)
   * @param val1 new A3 register value
   * @param val2 new A4 register value
   * @param state AT machine state
   */
  void set_A3_A4(long val1, long val2, AT_Machine_State state);

  /**
   * sets B1 from @addr (0x0116)
   * @param val new B1 register value
   * @param state AT machine state
   */
  void set_B1(long val, AT_Machine_State state);

  /**
   * sets B2 from @addr (0x0117)
   * @param val new B2 register value
   * @param state AT machine state
   */
  void set_B2(long val, AT_Machine_State state);

  /**
   * sets B3 from @addr (0x0118)
   * @param val new B3 register value
   * @param state AT machine state
   */
  void set_B3(long val, AT_Machine_State state);

  /**
   * sets B4 @addr (0x0119)
   * @param val new B4 register value
   * @param state AT machine state
   */
  void set_B4(long val, AT_Machine_State state);

  /**
   * sets B1 from @addr1 and B2 from @addr2 (0x011a)
   * @param val1 new B1 register value
   * @param val2 new B2 register value
   * @param state AT machine state
   */
  void set_B1_B2(long val1, long val2, AT_Machine_State state);

  /**
   * sets B3 from @addr3 and @addr4 to B4 (0x011b)
   * @param val3 new B3 register value
   * @param val4 new B4 register value
   * @param state AT machine state
   */
  void set_B3_B4(long val3, long val4, AT_Machine_State state);

  /**
   * sets A to zero (A being A1...4)
   * @param state AT machine state
   */
  void clear_A(AT_Machine_State state);

  /**
   * sets B to zero (B being B1...4)
   * @param state AT machine state
   */
  void clear_B(AT_Machine_State state);

  /**
   * gets A from B
   * @param state AT machine state
   */
  void copy_A_From_B(AT_Machine_State state);

  /**
   * gets B from A
   * @param state AT machine state
   *
   */
  void copy_B_From_A(AT_Machine_State state);

  /**
   * bool is A is zero
   * @param state AT machine state
   * @return boolean value (in a long?) if A is zero
   */
  long check_A_Is_Zero(AT_Machine_State state);

  /**
   * bool is B is zero
   * @param state AT machine state
   * @return boolean value (in a long?) if B is zero
   */
  long check_B_Is_Zero(AT_Machine_State state);


  /**
   * bool does A equal B
   * @param state AT machine state
   * @return boolean value (in a long?) if B is zero
   */
  long check_A_equals_B(AT_Machine_State state);

  /**
   * swap the values of A and B
   * @param state AT machine state
   */
  void swap_A_and_B(AT_Machine_State state);

  // note: these 8 math ops are intended for a future implementaion so no need to support them

  /**
   * adds A to B (result in B)
   * @param state AT machine state
   */
  void add_A_to_B(AT_Machine_State state);

  /**
   * add B to A (result in A)
   * @param state AT machine state
   */
  void add_B_to_A(AT_Machine_State state);

  /**
   * subs A from B (result in B)
   * @param state AT machine state
   */
  void sub_A_from_B(AT_Machine_State state);

  /**
   * subs B from A (result in A)
   * @param state AT machine state
   */
  void sub_B_from_A(AT_Machine_State state);

  /**
   * multiplies A by B (result in B)
   * @param state AT machine state
   */
  void mul_A_by_B(AT_Machine_State state);

  /**
   * multiplies B by A (result in A)
   * @param state AT machine state
   */
  void mul_B_by_A(AT_Machine_State state);

  /**
   * divides A by B (result in B) *can cause a divide by zero error which would stop the machine
   * @param state AT machine state
   */
  void div_A_by_B(AT_Machine_State state);

  /**
   * divides B by A (result in A) *can cause a divide by zero error which would stop the machine
   * @param state AT machine state
   */
  void div_B_by_A(AT_Machine_State state);

  /**
   * ors A by B (result in A)
   * @param state AT machine state
   */
  void or_A_with_B(AT_Machine_State state);

  /**
   * ors B by A (result in B)
   * @param state AT machine state
   */
  void or_B_with_A(AT_Machine_State state);

  /**
   * ands A by B (result in A)
   * @param state AT machine state
   */
  void and_A_with_B(AT_Machine_State state);

  /**
   * ands B by A (result in B)
   * @param state AT machine state
   */
  void and_B_with_A(AT_Machine_State state);

  /**
   * xors A by B (result in A)
   * @param state AT machine state
   */
  void xor_A_with_B(AT_Machine_State state);

  /**
   * xors B by A (result in B)
   * @param state AT machine state
   */
  void xor_B_with_A(AT_Machine_State state);

  // end range 0x0100..0x01ff

  // range 0x0200..0x02ff

  /**
   * sets @addr1 and @addr2 to the MD5 hash of A1..4
   * @param state AT machine state
   */
  void MD5_A_to_B(AT_Machine_State state);

  /**
   * bool if @addr1 and @addr2 matches the MD5 hash of A1..4
   * @param state AT machine state
   * @return bool if @addr1 and @addr2 matches the MD5 hash of A1..4
   */
  long check_MD5_A_with_B(AT_Machine_State state);

  /**
   * take a RIPEMD160 hash of A1..4 and put this in B1..4
   * @param state AT machine state
   */
  void HASH160_A_to_B(AT_Machine_State state);

  /**
   * bool if RIPEMD160 hash of A1..4 matches B1..4
   * @param state AT machine state
   * @return bool if RIPEMD160 hash of A1..4 matches B1..4
   */
  long check_HASH160_A_with_B(AT_Machine_State state);

  /**
   * take a SHA256 hash of A1..4 abd out this in B1..4
   * @param state AT machine state
   */
  void SHA256_A_to_B(AT_Machine_State state);

  /**
   * bool if SHA256 of A1..4 matches B1..4
   * @param state AT machine state
   * @return bool if SHA256 of A1..4 matches B1..4
   */
  long check_SHA256_A_with_B(AT_Machine_State state);

  // end of range 0x02..0x02ff

  // range 0x03..0x03ff

  /**
   * sets @addr to the timestamp of the current block
   * @param state AT machine state
   */
  long get_Block_Timestamp(AT_Machine_State state);

  /**
   * sets @addr to the timestamp of the AT creation block
   * @param state AT machine state
   */
  long get_Creation_Timestamp(AT_Machine_State state);


  /**
   * sets @addr to the timestamp of the previous block
   * @param state AT machine state
   * @return timestamp of the previous block
   */
  long get_Last_Block_Timestamp(AT_Machine_State state);

  /**
   * puts the block hash of the previous block in A
   * @param state AT machine state
   */
  void put_Last_Block_Hash_In_A(AT_Machine_State state);

  /**
   * sets A to zero/tx hash of the first tx after
   * @param state AT machine state
   */
  void A_to_Tx_after_Timestamp(long val, AT_Machine_State state);

  /**
   * @return bool if A is a valid tx with @addr to tx type
   * 0: normal tx
   * 1: message tx
   * @param state AT machine state
   */
  long get_Type_for_Tx_in_A(AT_Machine_State state);

  /**
   * @return bool if A is a valid tx with @addr to tx amount
   * @param state AT machine state
   */
  long get_Amount_for_Tx_in_A(AT_Machine_State state);

  /**
   * @return bool if A is a valid tx with @addr to the tx timestamp
   * @param state AT machine state
   */
  long get_Timestamp_for_Tx_in_A(AT_Machine_State state);

  /**
   * @return bool if A is a valid tx with @addr to the tx random id
   * random id is a 64bit signed value (always positive) and this is a blocking function
   * @param state AT machine state
   */
  long get_Random_Id_for_Tx_in_A(AT_Machine_State state);

  /**
   * bool if A is a valid tx with B to the tx message
   * if a tx is not a message tx then this will zero out the B value
   * @param state AT machine state
   */
  void message_from_Tx_in_A_to_B(AT_Machine_State state);

  /**
   * bool if A is a valid tx with B set to the tx address
   * @param state AT machine state
   */
  void B_to_Address_of_Tx_in_A(AT_Machine_State state);

  /**
   * set B to the address of the AT's creator
   * @param state AT machine state
   */
  void B_to_Address_of_Creator(AT_Machine_State state);

  // end range 0x0300..0x03ff
  // ------------------------

  // ------------------------
  // range 0x0400..0x04ff

  /**
   * sets @addr to current balance of the AT
   * @param state AT machine state
   */
  long get_Current_Balance(AT_Machine_State state);

  /**
   * sets @addr to the balance it had last had when running
   * this amount does not include any additional amounts sent to the
   * AT between "execution events"
   * @param state AT machine state
   */
  long get_Previous_Balance(AT_Machine_State state);

  /**
   * bool if B is a valid address then send it $addr amount
   * if this amount is greater than the AT's balance then it will also
   * return false
   * @param state AT machine state
   */
  void send_to_Address_in_B(long val, AT_Machine_State state);

  /**
   * bool if B is a valid address then send it entire balance
   * @param state AT machine state
   */
  void send_All_to_Address_in_B(AT_Machine_State state);

  /**
   * bool if B is a valid address then send it the old balance
   * @param state AT machine state
   */
  void send_Old_to_Address_in_B(AT_Machine_State state);

  /**
   * bool if B is valid address then send it A as a message
   * @param state AT machine state
   */
  void send_A_to_Address_in_B(AT_Machine_State state);

  /**
   * $addr1 is timestamp calculated from $addr2
   * @param state AT machine state
   * @param val1
   * @param val2
   * @return time+minutes
   */
  long add_Minutes_to_Timestamp(long val1, long val2, AT_Machine_State state);

  /**
   * set min amount of balance increase needed to unfreeze
   * @param state AT machine state
   * @param val
   */
  void set_Min_Activation_Amount(long val, AT_Machine_State state);

  // end range 0x0400.0x04ff
  // -----------------------

  /**
   * puts the gensig of the previous block in A
   * @param state AT machine state
   */
  void put_Last_Block_Generation_Signature_In_A(AT_Machine_State state);

  /**
   * take a SHA256 hash of val2 bytes starting at val1. out this in B1..4
   * @param state AT machine state
   * @param val1
   * @param val2
   */
  void SHA256_to_B(long val1, long val2, AT_Machine_State state);
}
