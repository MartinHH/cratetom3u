# CrateToM3U

A simple command-line tool to convert `.crate` files (created by
[Serato](https://serato.com/) software like Serato DJ, Scratch Live or
Itch) to `.m3u` playlist files.

(Note that currently, "smart crates" are not supported.)

## Installation

For those who just want to use this: if you're on a Mac and you have [Homebrew](https://brew.sh) installed, you can install CrateToM3U by simply entering:

`brew install martinhh/beta/cratetom3u`

(Sorry, no simple solution for Windows at the moment.)


## Usage

Enter `cratetom3u --help` for a list of all options:

```
MyMachine:~ myuser$ cratetom3u --help
cratetom3u 0.2.1
cratetom3u is a tool to convert Serato .crate files to .m3u playlist files.
(Please note that "smart crates" are not supported.)

Options:

  -a, --add  <prefix>           Audio file path prefix to prepend
  -b, --backslash               Replace all '/'s in audio file paths by '\'s
  -c, --charset  <charset>      Charset for the output files (default is your
                                system's default)
  -f, --filemode                Enable single file mode
  -m, --matches  <expression>   String that extracted .crate files must match
                                (supports regex) - irrelevant in single file
                                mode
  -r, --remove  <expression>    Audio file path substring to remove (supports
                                regex)
  -s, --suffix  <expression>    The suffix for the output files in directory
                                mode (including the leading '.' - default is
                                ".m3u") - irrelevant in single file mode
      --help                    Show help message
      --version                 Show version of this program

 trailing arguments:
  inputPath (required)    Path to input crates directory (or .crate file in
                          single file mode)
  outputPath (required)   Path to output directory (or .m3u file in single file
                          mode)
```

So a basic example to convert all crates from the default `Crates` would
 be this (writing the resulting `.m3u` files to `~/CrateM3Us/`):

```
MyMachine:~ myuser$ cratetom3u ~/Music/_Serato_/Crates/ ~/CrateM3Us/
```

However, since the audio file paths in `.crate` files are stored without
a leading `/` in front of the root directory, you might need to prepend
the missing `/` using the `-a` option:

```
MyMachine:~ myuser$ cratetom3u -a / ~/Music/_Serato_/Crates/ ~/CrateM3Us/
```

On a Mac with Serato installed to its default location, the above
command should create working `.m3u` files that can be used with VLC,
Itunes etc.

**For more details and examples: have a look at the [wiki](https://github.com/MartinHH/CrateToM3U/wiki).**