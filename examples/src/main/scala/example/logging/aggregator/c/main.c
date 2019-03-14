#include <stdio.h>
#include <zlog.h>

int main(int argc, char **argv)
{

    int rc;
    zlog_category_t* cat;
    rc = zlog_init("logging_default.conf"); // dzlog_init("test_default.conf", "my_cat");

    if (rc)
    {
        printf("Init failed. Either file name is incorrect or their is syntax error in configuration file.\n");
        return -1;
    }
    cat = zlog_get_category("my_cat");
    zlog_info(cat,"I am Info C Log");
    zlog_debug(cat,"I am Debug C Log");
    zlog_fatal(cat,"I am Fatal C Log");
    zlog_warn(cat,"I am warn C Log");
    zlog_error(cat, "I am Error C Log");
    zlog_fini();
    return 0;
}

//cc -c -o main.o main.c -I/usr/local/include
//cc -o main main.o -L/usr/local/lib -lzlog