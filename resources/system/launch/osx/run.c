#include <limits.h>         /* PATH_MAX */
#include <mach-o/dyld.h>    /* _NSGetExecutablePath */
#include <libgen.h>         /* dirname */
#include <string.h>         /* strcpy */
#include <unistd.h>         /* execv */
#include <stdio.h>

int main(int argc, char** argv) {
    unsigned int size = PATH_MAX+1;
    char origpath[size];
    char newpath[size];
    char *dir;

    char * newexec = "/JavaApplicationStub";

    _NSGetExecutablePath(origpath, &size);
    strlcpy(newpath, dirname(origpath), size);
    strlcat(newpath, newexec, size);
    execv(newpath, argv);
}
