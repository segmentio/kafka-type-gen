package main

import (
	"bytes"
	"flag"
	"fmt"
	"go/ast"
	"go/parser"
	"go/token"
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

		printf := func(format string, args ...interface{}) {
			fmt.Fprintf(output, format, args...)
		}

		// The first pass aggregates all versionned types under a common base name.
		// We do this to have access to all the fields that the high-level types
		// must contain.
		types := map[string][]*ast.TypeSpec{}
		baseTypeRegexp := regexp.MustCompile(`(.*)V[1-9]+$`)

		ast.Inspect(f, func(n ast.Node) bool {
			switch x := n.(type) {
			case *ast.TypeSpec:
				typeName := x.Name.Name
				if baseTypeRegexp.MatchString(typeName) {
					baseTypeName := baseTypeRegexp.ReplaceAllString(typeName, "$1")
					types[baseTypeName] = append(types[baseTypeName], x)
				}
			}
			return true
		})

		// Sorted list of all type names, used to make the output deterministic.
		baseTypeNames := make([]string, 0, len(types))
		for baseTypeName := range types {
			baseTypeNames = append(baseTypeNames, baseTypeName)
		}
		sort.Strings(baseTypeNames)

		// Generate the type definition; we match the last type because there are
		// very few cases where fields were removed in the kafka protocol, and it
		// seems more sane to avoid building support for deprecated features.
		for _, baseTypeName := range baseTypeNames {
			typeList := types[baseTypeName]
			lastTypeVersion := typeList[len(typeList)-1]

			printf("type %s struct {\n", baseTypeName)

			for _, field := range lastTypeVersion.Type.(*ast.StructType).Fields.List {
				for _, ident := range field.Names {
					fieldName := ident.Name
					fieldType := typeNameOf(field.Type)
					printf("%s %s\n", fieldName, fieldType)
				}
			}

			printf("}\n\n")
		}

		if err := ioutil.WriteFile(filePath, output.Bytes(), 0644); err != nil {
			panic(err)
		}
	}
}

func typeNameOf(expr ast.Expr) string {
	switch x := expr.(type) {
	case *ast.Ident:
		return x.Name
	case *ast.ArrayType:
		return "[]" + typeNameOf(x.Elt)
	default:
		panic(x)
	}
}
