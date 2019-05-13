#include "cbor.h"
#include <stdio.h>

/*
 * Reads data from a file. Example file:
 * event.cbor
 */
void print_cbor(cbor_item_t* item, int indent);
int main(int argc, char * argv[])
{
    FILE * f = fopen("../data/event.cbor", "rb");

    /* Calculate size of the file */
    fseek(f, 0, SEEK_END);
    size_t length = (size_t)ftell(f);
    fseek(f, 0, SEEK_SET);

    /* read the file content in a byte-sized buffer */
    unsigned char * buffer = malloc(length);
    fread(buffer, length, 1, f);

    /* High-level decoding result is stored here */
    struct cbor_load_result result;

    /* All items are stored recusrsively (struct of struct of .... ) */
    cbor_item_t * item = cbor_load(buffer, length, &result);

    print_cbor(item,0);

    /* Default printing of the result */
    /* cbor_describe(item, stdout);
     fflush(stdout);
    */

    /* Deallocate the result */
    cbor_decref(&item);

    /* Close the file pointer */
    fclose(f);
}

/* Pretty prints CBOR in JSON like format */
void print_cbor(cbor_item_t *item,int indent)
{
    switch (cbor_typeof(item)) {
    case CBOR_TYPE_UINT: {
      printf("%llu", cbor_get_int(item));
      break;
    };
    case CBOR_TYPE_NEGINT: {
      printf("-%llu -1\n", cbor_get_int(item));
      break;
    };
    case CBOR_TYPE_BYTESTRING: {
       /* Do not know what to do*/
      break;
    };
    case CBOR_TYPE_STRING: {
        fwrite(cbor_string_handle(item), (int)cbor_string_length(item), 1, stdout);
      break;
    };
    case CBOR_TYPE_ARRAY: {
        printf("{");
      for (size_t i = 0; i < cbor_array_size(item); i++)
      {
        print_cbor(cbor_array_handle(item)[i],indent+4);
        printf(",");
      }
      printf(" }");
      break;
    };
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
    };
    case CBOR_TYPE_TAG: {
      fprintf(stdout, "%llu \n", cbor_tag_value(item));
      print_cbor(cbor_tag_item(item), indent+4);
      break;
    };
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
    };
  }
}


/* Compilation of above program */
// gcc sample.c -lcbor -o serialized-cbor