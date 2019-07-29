cmake_minimum_required(VERSION 3.10.2)

add_library(dispatcher-wrapper SHARED src/main/cpp/dispatcher-wrapper.c)

set(scion_C_DIR src/main/cpp/gobind-scion/c)
add_library(dispatcher STATIC ${scion_C_DIR}/dispatcher/dispatcher.c)

set(zlog_src_DIR src/main/cpp/zlog/src)
add_library(zlog STATIC
    ${zlog_src_DIR}/spec.c
    ${zlog_src_DIR}/event.c
#    ${zlog_src_DIR}/zlog-chk-conf.c
    ${zlog_src_DIR}/category_table.c
    ${zlog_src_DIR}/zc_arraylist.c
    ${zlog_src_DIR}/level_list.c
    ${zlog_src_DIR}/thread.c
    ${zlog_src_DIR}/rotater.c
    ${zlog_src_DIR}/format.c
    ${zlog_src_DIR}/zlog.c
    ${zlog_src_DIR}/category.c
    ${zlog_src_DIR}/conf.c
    ${zlog_src_DIR}/buf.c
    ${zlog_src_DIR}/record_table.c
    ${zlog_src_DIR}/record.c
    ${zlog_src_DIR}/zc_profile.c
    ${zlog_src_DIR}/mdc.c
    ${zlog_src_DIR}/zc_hashtable.c
    ${zlog_src_DIR}/zc_util.c
    ${zlog_src_DIR}/rule.c
    ${zlog_src_DIR}/level.c
)
target_include_directories(zlog PUBLIC ${zlog_src_DIR})
target_link_libraries(dispatcher zlog)

set(scion_src_DIR ${scion_C_DIR}/lib/scion)
add_library(scion STATIC
#    ${scion_src_DIR}/checksum_bench.c
    ${scion_src_DIR}/packet.c
    ${scion_src_DIR}/checksum.c
    ${scion_src_DIR}/path.c
    ${scion_src_DIR}/udp.c
    ${scion_src_DIR}/address.c
    ${scion_src_DIR}/utils.c
    ${scion_src_DIR}/extensions.c
    ${scion_src_DIR}/scmp.c
    ${scion_src_DIR}/opaque_field.c
)

add_definitions(-D_DEFINES_H_)

add_definitions(-DSCIOND_API_PORT=3333)
add_definitions(-DDISPATCHER_DIR="run/shm/dispatcher")
add_definitions(-DDEFAULT_DISPATCHER_ID="default")

add_definitions(-DDISPATCHER_BUF_SIZE=65535)

add_definitions(-DSCION_UDP_EH_DATA_PORT=30041)
add_definitions(-DSCION_FILTER_CMD_PORT=30042)
add_definitions(-DSCION_ROUTER_PORT=50000)

add_definitions(-DL4_PROTOCOL_COUNT=3)
add_definitions(-DL4_NONE=0)
add_definitions(-DL4_SCMP=1)
add_definitions(-DL4_TCP=6)
add_definitions(-DL4_UDP=17)

add_definitions(-DMAX_HOST_ADDR_LEN=16)

add_definitions(-DIFID_LEN=2)

add_definitions(-DREV_TOKEN_LEN=32)
add_definitions(-DAD_MARKING_METADATA_LEN=8)
add_definitions(-DPCB_MARKING_LEN=44) #(12 + REV_TOKEN_LEN)

add_definitions(-DMAX_SEGMENT_TTL=86400) #(24 * 60 * 60)
add_definitions(-DEXP_TIME_UNIT=337) #(MAX_SEGMENT_TTL >> 8)

add_definitions(-DLINE_LEN=8)
target_include_directories(dispatcher PUBLIC ${scion_C_DIR}/lib)
target_include_directories(dispatcher PUBLIC src/main/cpp/uthash/src)
target_link_libraries(dispatcher scion)


target_link_libraries(dispatcher-wrapper dispatcher)
