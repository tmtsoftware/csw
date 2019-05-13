#include "cbor.h"
#include <stdio.h>
#include <string.h>

/*
 * Basic C Example for CBOR Utility:
 *
 * Encodes CSW Event, serialises into cbor and deserialises as a Map(Key -> Value)
 */


struct utctime {
    long seconds;
    long nanos;
};

struct event {
    char* eventId;
    char* source;
    char* eventName;
    struct utctime eventTime;
};

/* Encode the time structure with the right keys */
cbor_item_t* encode_time(struct utctime time)
{
    cbor_item_t* utc = cbor_new_definite_map(2);
    cbor_map_add(utc, (struct cbor_pair) {
            .key = cbor_move(cbor_build_string("seconds")),
            .value = cbor_move(cbor_build_uint64(time.seconds))
    });

    cbor_map_add(utc, (struct cbor_pair) {
            .key = cbor_move(cbor_build_string("nanos")),
            .value = cbor_move(cbor_build_uint64(time.nanos))
    });
    return utc;
}

/* Encode the event structure with the right keys */
cbor_item_t* encode_event(struct event e)
{
    cbor_item_t* root = cbor_new_definite_array(2);
    cbor_array_push(root, cbor_move(cbor_build_string("SystemEvent")));

    cbor_item_t* eve = cbor_new_definite_map(5);
    cbor_map_add(eve, (struct cbor_pair) {
            .key = cbor_move(cbor_build_string("eventId")),
            .value = cbor_move(cbor_build_string(e.eventId))
    });

    cbor_map_add(eve, (struct cbor_pair) {
            .key = cbor_move(cbor_build_string("source")),
            .value = cbor_move(cbor_build_string(e.source))
    });

    cbor_map_add(eve, (struct cbor_pair) {
            .key = cbor_move(cbor_build_string("eventName")),
            .value = cbor_move(cbor_build_string(e.eventName))
    });

    cbor_map_add(eve, (struct cbor_pair) {
            .key = cbor_move(cbor_build_string("eventTime")),
            .value = encode_time(e.eventTime)
    });

    cbor_map_add(eve, (struct cbor_pair) {
            .key = cbor_move(cbor_build_string("paramSet")),
            .value = cbor_new_definite_array(0)
    });

    cbor_array_push(root, eve);
    return root;
}

/* Serialize the event structure into the `stream` file-stream, post encoding */
void serialize(struct event e, FILE* stream)
{
    cbor_item_t* root = encode_event(e);
    unsigned char * buffer;
    size_t buffer_size, length = cbor_serialize_alloc(root, &buffer, &buffer_size);
    fwrite(buffer, 1, length, stream);
    free(buffer);
    cbor_decref(&root);
}

/* Read the content of cbor file using libcbor load method */
cbor_item_t *read_cbor_file(const char *file) {

    unsigned char *buffer;

    /* Calculate size of the file */
    FILE* f = fopen(file,"rb");
    fseek(f, 0, SEEK_END);
    size_t length = (size_t) ftell(f);
    fseek(f, 0, SEEK_SET);

    buffer = malloc(length);
    /* read the file content in a byte-sized buffer */
    fread(buffer, length, 1, f);
    fclose(f);

    /* High-level decoding result is stored here */
    struct cbor_load_result result;

    /* All items are stored recusrsively (struct of struct of .... ) */
    cbor_item_t * item = cbor_load(buffer, length, &result);

    return item;
}

/* Pretty prints CBOR in JSON like format */
void print_cbor(cbor_item_t *item,int indent)
{
    switch (cbor_typeof(item)) {
        case CBOR_TYPE_UINT: {
            printf("%llu", cbor_get_int(item));
            break;
        }
        case CBOR_TYPE_NEGINT: {
            printf("-%llu -1\n", cbor_get_int(item));
            break;
        }
        case CBOR_TYPE_BYTESTRING: {
            /* for (size_t i = 0; i < cbor_bytestring_chunk_count(item); i++)
               _cbor_nested_describe(cbor_bytestring_chunks_handle(item)[i], out,
                                     indent + 4);
           } else {
             fprintf(out, "Definite, length %zuB\n", cbor_bytestring_length(item));
           }*/
            break;
        }
        case CBOR_TYPE_STRING: {
            fwrite(cbor_string_handle(item), (int)cbor_string_length(item), 1, stdout);
            break;
        }
        case CBOR_TYPE_ARRAY: {
            printf("{");
            for (size_t i = 0; i < cbor_array_size(item); i++)
            {
                print_cbor(cbor_array_handle(item)[i],indent+4);
                printf(",");
            }
            printf(" }");
            break;
        }
        case CBOR_TYPE_MAP: {
            fprintf(stdout, "%*s", indent, " ");
            printf("{\n");
            fprintf(stdout, "%*s", indent, " ");
            for (size_t i = 0; i < cbor_map_size(item); i++) {
                fprintf(stdout, "%*s", indent, " ");
                print_cbor(cbor_map_handle(item)[i].key, indent+4);
                printf(" -> ");
                print_cbor(cbor_map_handle(item)[i].value,indent+4);
                printf(",\n");
                fprintf(stdout, "%*s", indent, " ");
            }
            fprintf(stdout, "%*s", indent, " ");
            printf(" }\n");
            break;
        }
        case CBOR_TYPE_TAG: {
            fprintf(stdout, "%llu \n", cbor_tag_value(item));
            print_cbor(cbor_tag_item(item), indent+4);
            break;
        }
        case CBOR_TYPE_FLOAT_CTRL: {
            if (cbor_float_ctrl_is_ctrl(item)) {
                if (cbor_is_bool(item))
                    printf("%s",
                           cbor_ctrl_is_bool(item) ? "true" : "false");
                else if (cbor_is_undef(item))
                    printf("Undefined");
                else if (cbor_is_null(item))
                    printf("Null");
                else
                    printf("%d", cbor_ctrl_value(item));
            } else {
                printf("%lf", cbor_float_get_float(item));
            }
            break;
        }
    }
}


int main(int argc, char * argv[])
{
    struct utctime time = {1323928,98946};
    struct event e = {"id1","a.b","ev1",time};

    FILE* test = fopen("test.cbor","wb");
    serialize(e, test);
    fclose(test);

    /* input.cbor is serialised using scala cbor library */
//    cbor_item_t * item = read_cbor_file("../input.cbor");

    cbor_item_t * item = read_cbor_file("test.cbor");

    /* Rudimentary pretty print of de-serialised cbor file using self created method. Prints on console only */
    print_cbor(item,0);

    /* Default printing of the result using cbor print library  */
//      cbor_describe(item, stdout);
//      fflush(stdout);

    /* Deallocate the result */
    cbor_decref(&item);
    return 0;
}


/* Compilation of above program using command line */
// gcc main.c -lcbor -o serialized-cbor