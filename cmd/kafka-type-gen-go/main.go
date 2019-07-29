package main

import (
	"bytes"
	"flag"
	"fmt"
	"go/ast"
	"go/parser"
	"go/token"
	"io"
	"io/ioutil"
	"regexp"
	"sort"
)

func main() {
	flag.Parse()

	for _, filePath := range flag.Args() {
		fset := token.NewFileSet()

		b, err := ioutil.ReadFile(filePath)
		if err != nil {
			panic(err)
		}

		f, err := parser.ParseFile(fset, filePath, string(b), 0)
		if err != nil {
			panic(err)
		}

		// We regenerate the file in-place, so all its previous content must be
		// retained as well.
		output := &bytes.Buffer{}
		output.Grow(2 * len(b))
		output.Write(b)

		// The first pass aggregates all versionned types under a common base name.
		// We do this to have access to all the fields that the high-level types
		// must contain.
		baseTypes := map[string][]*ast.TypeSpec{}
		baseTypeRegexp := regexp.MustCompile(`(.+)V[1-9]+$`)
		subTypes := map[string][]*ast.TypeSpec{}
		subTypeRegexp := regexp.MustCompile(`(.+)V[1-9]+(.+)`)
		revSubTypes := map[string]string{}

		ast.Inspect(f, func(n ast.Node) bool {
			switch x := n.(type) {
			case *ast.TypeSpec:
				typeName := x.Name.Name
				switch {
				case baseTypeRegexp.MatchString(typeName):
					baseTypeName := baseTypeRegexp.ReplaceAllString(typeName, "$1")
					baseTypes[baseTypeName] = append(baseTypes[baseTypeName], x)
				case subTypeRegexp.MatchString(typeName):
					subTypeName := subTypeRegexp.ReplaceAllString(typeName, "$1$2")
					subTypes[subTypeName] = append(subTypes[subTypeName], x)
					revSubTypes[typeName] = subTypeName
				}
			}
			return true
		})

		// Generate the type definition; we match the last type because there are
		// very few cases where fields were removed in the kafka protocol, and it
		// seems more sane to avoid building support for deprecated features.
		printTypes(output, baseTypes, revSubTypes)
		printTypes(output, subTypes, revSubTypes)

		if err := ioutil.WriteFile(filePath, output.Bytes(), 0644); err != nil {
			panic(err)
		}
	}
}

func printTypes(output io.Writer, types map[string][]*ast.TypeSpec, replace map[string]string) {
	typeNames := make([]string, 0, len(types))
	for typeName := range types {
		typeNames = append(typeNames, typeName)
	}
	sort.Strings(typeNames)

	printf := func(format string, args ...interface{}) {
		fmt.Fprintf(output, format, args...)
	}

	for _, typeName := range typeNames {
		typeList := types[typeName]
		lastTypeVersion := typeList[len(typeList)-1]

		printf("type %s struct {\n", typeName)

		for _, field := range lastTypeVersion.Type.(*ast.StructType).Fields.List {
			for _, ident := range field.Names {
				fieldName := ident.Name
				fieldType := typeNameOf(field.Type, replace)
				printf("%s %s\n", fieldName, fieldType)
			}
		}

		printf("}\n\n")
	}
}

func typeNameOf(expr ast.Expr, replace map[string]string) string {
	switch x := expr.(type) {
	case *ast.Ident:
		name := x.Name
		repl := replace[name]
		if repl != "" {
			return repl
		}
		return name
	case *ast.ArrayType:
		return "[]" + typeNameOf(x.Elt, replace)
	default:
		panic(x)
	}
}
