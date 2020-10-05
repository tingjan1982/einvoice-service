create table turnkey_message_log (
  seqno character varying(8) not null,
  subseqno character varying(5) not null,
  uuid character varying(40) default null,
  message_type character varying(10) default null,
  category_type character varying(5) default null,
  process_type character varying(10) default null,
  from_party_id character varying(10) default null,
  to_party_id character varying(10) default null,
  message_dts character varying(17) default null,
  character_count character varying(10) default null,
  status character varying(5) default null,
  in_out_bound character varying(1) default null,
  from_routing_id character varying(39) default null,
  to_routing_id character varying(39) default null,
  invoice_identifier character varying(30) default null,
  constraint turnkey_message_log_pk1 primary key (seqno, subseqno)
)
;
